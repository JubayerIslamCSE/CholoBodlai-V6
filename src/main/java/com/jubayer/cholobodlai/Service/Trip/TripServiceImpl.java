package com.jubayer.cholobodlai.Service.Trip;

import com.jubayer.cholobodlai.Component.Mapper.TripMapper;
import com.jubayer.cholobodlai.Component.Notification.NotificationService;
import com.jubayer.cholobodlai.Component.Pricing.FareEstimationService;
import com.jubayer.cholobodlai.DTO.*;
import com.jubayer.cholobodlai.EXCEPTION.*;
import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Model.Truck.*;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Repository.Truck.*;
import com.jubayer.cholobodlai.Service.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TripServiceImpl implements TripService {
    private final TripRepository trips;
    private final TruckTypeRepository types;
    private final UserRepository users;
    private final BidRepository bids;
    private final TruckRepository trucks;
    private final FareEstimationService pricing;
    private final TripMapper mapper;
    private final CurrentUserService current;
    private final NotificationService notifications;
    private final PickupTimePolicy pickupTimePolicy;

    public TripServiceImpl(TripRepository trips, TruckTypeRepository types, UserRepository users,
            BidRepository bids, TruckRepository trucks, FareEstimationService pricing,
            TripMapper mapper, CurrentUserService current, NotificationService notifications,
            PickupTimePolicy pickupTimePolicy) {
        this.trips = trips; this.types = types; this.users = users; this.bids = bids;
        this.trucks = trucks; this.pricing = pricing; this.mapper = mapper;
        this.current = current; this.notifications = notifications;
        this.pickupTimePolicy = pickupTimePolicy;
    }

    @Override @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public TripResponse create(TripRequest request) { return createFor(request, null); }

    @Override @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    @Transactional
    public TripResponse createFor(TripRequest req, String shipperId) {
        User actor = current.currentUser();
        User shipper = actor;
        if (actor.getRole() == Role.ADMIN) {
            if (shipperId == null || shipperId.isBlank())
                throw new BusinessRuleException("Choose a shipper account");
            shipper = users.findById(shipperId).orElseThrow(() ->
                    new ResourceNotFoundException("User", shipperId));
            if (shipper.getRole() != Role.SHIPPER || !shipper.isEnabled())
                throw new BusinessRuleException("Choose an active shipper");
        }
        TruckType type = validType(req);
        validate(req);
        checkTruckCapacity(req, type);
        LocalDateTime now = LocalDateTime.now();
        Trip trip = Trip.builder().shipperId(shipper.getId())
                .shipperName(shipper.getFullName())
                .requestedTypeId(type.getId()).requestedTypeName(type.getName())
                .baseRatePerKm(type.getBaseRatePerKm())
                .pickupLocation(req.pickupLocation().trim()).dropoffLocation(req.dropoffLocation().trim())
                .distanceKm(req.distanceKm()).productDetails(req.productDetails())
                .cargoWeightKg(req.cargoWeightKg())
                .loadingTime(req.loadingTime()).roundTrip(req.roundTrip())
                .status(TripStatus.OPEN).createdAt(now).updatedAt(now).build();
        trip.setEstimatedFare(pricing.estimate(trip));
        Trip saved = trips.insert(trip);
        // Notify eligible approved owners. This is a demo-scale query; use pagination / events at scale.
        for (Truck truck : trucks.findByStatus(TruckStatus.AVAILABLE)) {
            if (truck.isVerified() && type.getId().equals(truck.getTruckTypeId())) {
                users.findById(truck.getOwnerId()).filter(u -> u.isEnabled()
                        && u.effectiveApprovalStatus() == ApprovalStatus.APPROVED)
                        .ifPresent(u -> notifications.notify(u, "New matching trip request: "
                                + trip.getPickupLocation() + " to " + trip.getDropoffLocation()));
            }
        }
        return mapper.toResponse(saved);
    }

    @Override @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    @Transactional
    public TripResponse update(String id, TripRequest req) {
        Trip trip = find(id);
        mustBeShipperOrAdmin(trip);
        if (trip.getStatus() != TripStatus.OPEN)
            throw new BusinessRuleException("Only open trip requests may be edited");
        if (!bids.findByTripIdAndStatus(id, BidStatus.PENDING).isEmpty())
            throw new BusinessRuleException("This request has offers. Cancel it and create a new request to change the details");
        TruckType type = validType(req);
        validate(req);
        checkTruckCapacity(req, type);
        trip.setRequestedTypeId(type.getId()); trip.setRequestedTypeName(type.getName());
        trip.setBaseRatePerKm(type.getBaseRatePerKm());
        trip.setPickupLocation(req.pickupLocation().trim());
        trip.setDropoffLocation(req.dropoffLocation().trim());
        trip.setDistanceKm(req.distanceKm()); trip.setCargoWeightKg(req.cargoWeightKg());
        trip.setProductDetails(req.productDetails()); trip.setLoadingTime(req.loadingTime());
        trip.setRoundTrip(req.roundTrip()); trip.setEstimatedFare(pricing.estimate(trip));
        trip.setUpdatedAt(LocalDateTime.now());
        return mapper.toResponse(trips.save(trip));
    }

    private TruckType validType(TripRequest r) {
        if (r.truckTypeId() == null || r.truckTypeId().isBlank())
            throw new BusinessRuleException("Choose a truck type");
        return types.findById(r.truckTypeId()).orElseThrow(() ->
                new ResourceNotFoundException("TruckType", r.truckTypeId()));
    }
    private void checkTruckCapacity(TripRequest req, TruckType type) {
        if (req.cargoWeightKg() != null && type.getCapacityTons() != null &&
                req.cargoWeightKg() > type.getCapacityTons() * 1000.0) {
            throw new BusinessRuleException("Cargo weight exceeds the selected truck type capacity");
        }
    }
    private void validate(TripRequest r) {
        pickupTimePolicy.validate(r.loadingTime());
        if (r.pickupLocation() == null || r.pickupLocation().isBlank() ||
                r.dropoffLocation() == null || r.dropoffLocation().isBlank() ||
                r.distanceKm() == null || !Double.isFinite(r.distanceKm()) || r.distanceKm() <= 0 || r.distanceKm() > 10000)
            throw new BusinessRuleException("Enter valid pickup, destination and distance");
        if (r.pickupLocation() != null && r.dropoffLocation() != null &&
                r.pickupLocation().trim().equalsIgnoreCase(r.dropoffLocation().trim()))
            throw new BusinessRuleException("Pickup and destination must be different places");
        if (r.cargoWeightKg() != null && (!Double.isFinite(r.cargoWeightKg()) || r.cargoWeightKg() < 0.1 || r.cargoWeightKg() > 50000))
            throw new BusinessRuleException("Cargo weight must be between 0.1 and 50,000 kg");
        if (r.pickupLocation().length() > 180 || r.dropoffLocation().length() > 180 ||
                (r.productDetails() != null && r.productDetails().length() > 1500))
            throw new BusinessRuleException("Location or cargo description is too long");
    }

    @Override
    public TripResponse getById(String id) {
        Trip t = find(id);
        User actor = current.currentUser();
        boolean acceptedOwner = actor.getRole() == Role.OWNER &&
                bids.findByTripIdAndStatus(id, BidStatus.ACCEPTED).stream()
                        .anyMatch(b -> actor.getId().equals(b.getOwnerId()));
        boolean authorized = MarketplaceRules.canSeeTrip(
                actor.getRole(), actor.getId(), t.getShipperId(),
                t.getStatus() == TripStatus.OPEN, acceptedOwner);
        if (!authorized) throw new AccessDeniedException("You cannot view this trip");
        return mapper.toResponse(t);
    }

    @Override
    public List<TripResponse> getAll() {
        User a = current.currentUser();
        if (a.getRole() == Role.ADMIN) return trips.findAll().stream().map(mapper::toResponse).toList();
        if (a.getRole() == Role.SHIPPER) return trips.findByShipperId(a.getId())
                .stream().map(mapper::toResponse).toList();
        LinkedHashMap<String, Trip> visible = new LinkedHashMap<>();
        for (Trip t : trips.findByStatus(TripStatus.OPEN)) visible.put(t.getId(), t);
        for (Bid b : bids.findByOwnerIdAndStatus(a.getId(), BidStatus.ACCEPTED)) {
            trips.findById(b.getTripId()).ifPresent(t -> visible.put(t.getId(), t));
        }
        return visible.values().stream().map(mapper::toResponse).toList();
    }

    @Override @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<TripResponse> getOpenTrips() {
        return trips.findByStatus(TripStatus.OPEN).stream().map(mapper::toResponse).toList();
    }

    @Override @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')") @Transactional
    public void cancel(String id) {
        Trip trip = find(id);
        mustBeShipperOrAdmin(trip);
        if (trip.getStatus() != TripStatus.OPEN)
            throw new BusinessRuleException("Accepted trips require an administrator to resolve cancellation");
        trip.setStatus(TripStatus.CANCELLED); trip.setUpdatedAt(LocalDateTime.now());
        trips.save(trip);
        for (Bid b : bids.findByTripIdAndStatus(id, BidStatus.PENDING)) {
            b.setStatus(BidStatus.WITHDRAWN); bids.save(b);
            users.findById(b.getOwnerId()).ifPresent(u -> notifications.notify(u, "Trip request was cancelled by the shipper"));
        }
    }

    @Override @PreAuthorize("hasAnyRole('OWNER','ADMIN')") @Transactional
    public void advance(String id) {
        Trip trip = find(id);
        User a = current.currentUser();
        Bid winner = bids.findByTripIdAndStatus(id, BidStatus.ACCEPTED).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("No winning offer on this trip"));
        if (a.getRole() != Role.ADMIN && !a.getId().equals(winner.getOwnerId()))
            throw new AccessDeniedException("Only the winning owner can update the delivery");
        TripStatus next = switch (trip.getStatus()) {
            case ASSIGNED -> TripStatus.PICKUP_READY;
            case PICKUP_READY -> TripStatus.IN_TRANSIT;
            case IN_TRANSIT -> TripStatus.DELIVERED;
            default -> throw new BusinessRuleException("This trip cannot advance from its current status");
        };
        trip.setStatus(next); trip.setUpdatedAt(LocalDateTime.now());
        if (next == TripStatus.DELIVERED) trip.setDeliveredAt(LocalDateTime.now());
        trips.save(trip);
        users.findById(trip.getShipperId()).ifPresent(u -> notifications.notify(u,
                "Delivery status updated to " + next.name().replace('_',' ')));
    }

    @Override @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')") @Transactional
    public void complete(String id) {
        Trip trip = find(id);
        mustBeShipperOrAdmin(trip);
        User actor = current.currentUser();
        if (trip.getStatus() != TripStatus.DELIVERED &&
                !(actor.getRole() == Role.ADMIN && EnumSet.of(TripStatus.ASSIGNED,
                        TripStatus.PICKUP_READY, TripStatus.IN_TRANSIT).contains(trip.getStatus()))) {
            throw new BusinessRuleException("Wait for the owner to mark the delivery delivered before confirming");
        }
        Truck truck = trucks.findById(trip.getAssignedTruckId()).orElseThrow(() ->
                new ResourceNotFoundException("Truck", trip.getAssignedTruckId()));
        trip.setStatus(TripStatus.COMPLETED); trip.setCompletedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now()); trips.save(trip);
        truck.setStatus(TruckStatus.AVAILABLE); trucks.save(truck);
        for (Bid b : bids.findByTripIdAndStatus(id, BidStatus.ACCEPTED)) {
            users.findById(b.getOwnerId()).ifPresent(u -> notifications.notify(u, "Delivery completed! You can accept new jobs"));
        }
    }

    private void mustBeShipperOrAdmin(Trip trip) {
        User a = current.currentUser();
        if (a.getRole() != Role.ADMIN && !a.getId().equals(trip.getShipperId()))
            throw new AccessDeniedException("Only the trip's shipper or an administrator may do this");
    }
    private Trip find(String id) {
        return trips.findById(id).orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }
}
