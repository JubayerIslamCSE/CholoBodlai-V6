
package com.jubayer.cholobodlai.Service.Bid;

import com.jubayer.cholobodlai.Component.Mapper.BidMapper;
import com.jubayer.cholobodlai.Component.Notification.NotificationService;
import com.jubayer.cholobodlai.DTO.*;
import com.jubayer.cholobodlai.EXCEPTION.*;
import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Model.Truck.Truck;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import com.jubayer.cholobodlai.Service.CurrentUserService;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BidServiceImpl implements BidService {

    private final BidRepository bids;
    private final TripRepository trips;
    private final TruckRepository trucks;
    private final UserRepository users;
    private final NotificationService notifications;
    private final BidMapper mapper;
    private final CurrentUserService current;
    private final MongoTemplate mongo;

    public BidServiceImpl(
            BidRepository bids,
            TripRepository trips,
            TruckRepository trucks,
            UserRepository users,
            NotificationService notifications,
            BidMapper mapper,
            CurrentUserService current,
            MongoTemplate mongo) {

        this.bids = bids;
        this.trips = trips;
        this.trucks = trucks;
        this.users = users;
        this.notifications = notifications;
        this.mapper = mapper;
        this.current = current;
        this.mongo = mongo;
    }

    @Override
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Transactional
    public BidResponse placeBid(BidRequest req) {

        User actor = current.currentUser();

        Trip trip = trips.findById(req.tripId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip", req.tripId()));

        if (trip.getStatus() != TripStatus.OPEN) {
            throw new BusinessRuleException(
                    "Trip is no longer accepting bids");
        }

        Truck truck = trucks.findById(req.truckId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Truck", req.truckId()));

        if (!com.jubayer.cholobodlai.Service.MarketplaceRules.canBidWithTruck(
                actor.getRole(), actor.getId(), truck.getOwnerId())) {

            throw new AccessDeniedException(
                    "You can bid only with your registered truck");
        }

        if (!truck.isVerified()
                || truck.getStatus() != TruckStatus.AVAILABLE) {

            throw new BusinessRuleException(
                    "Truck is unavailable or not approved");
        }

        if (!trip.getRequestedTypeId().equals(
                truck.getTruckTypeId())) {

            throw new BusinessRuleException(
                    "Truck type does not match the shipper request");
        }

        com.jubayer.cholobodlai.Service.MarketplaceRules.bid(req.amount());

        User owner = users.findById(truck.getOwnerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User", truck.getOwnerId()));

        if (owner.getRole() != Role.OWNER
                || !owner.isEnabled()
                || owner.effectiveApprovalStatus()
                != ApprovalStatus.APPROVED) {

            throw new BusinessRuleException(
                    "Truck owner is not approved");
        }

        LocalDateTime now = LocalDateTime.now();

        Optional<Bid> old =
                bids.findFirstByTripIdAndTruckIdOrderByCreatedAtAsc(
                        trip.getId(), truck.getId());

        Bid bid;

        if (old.isPresent()) {

            // Update the previous bid.
            bid = old.get();

            if (bid.getStatus() != BidStatus.PENDING && bid.getStatus() != BidStatus.WITHDRAWN) {
                throw new BusinessRuleException("This offer is no longer editable");
            }
            // Reactivate the same MongoDB document if an earlier offer was withdrawn.
            bid.setStatus(BidStatus.PENDING);
            bid.setAmount(req.amount());
            bid.setUpdatedAt(now);

            bid = bids.save(bid);

        } else {

            // Create the first bid.
            bid = Bid.builder()
                    .id("bid:" + trip.getId() + ":" + truck.getId())
                    .tripId(trip.getId())
                    .truckId(truck.getId())
                    .ownerId(owner.getId())
                    .ownerName(owner.getFullName())
                    .truckRegistrationNo(truck.getRegistrationNo())
                    .amount(req.amount())
                    .status(BidStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            try {
                bid = bids.insert(bid);

            } catch (DuplicateKeyException ex) {

                throw new BusinessRuleException(
                        "Another bid was just submitted. Refresh and retry");
            }
        }

        users.findById(trip.getShipperId()).ifPresent(u ->
                notifications.notify(
                        u, "Offer updated on your trip request"));

        return mapper.toResponse(bid);
    }

    @Override
    public List<BidResponse> getBidsForTrip(String tripId) {

        Trip trip = trips.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip", tripId));

        User actor = current.currentUser();

        boolean admin = actor.getRole() == Role.ADMIN;

        boolean shipper =
                actor.getRole() == Role.SHIPPER
                        && trip.getShipperId().equals(actor.getId());

        if (!admin && !shipper
                && actor.getRole() != Role.OWNER) {

            throw new AccessDeniedException(
                    "Bids are private");
        }

        BidStatus visibleStatus =
                trip.getStatus() == TripStatus.OPEN
                        ? BidStatus.PENDING
                        : BidStatus.ACCEPTED;

        // Show only one bid per truck.
        Map<String, Bid> perTruck = new LinkedHashMap<>();

        for (Bid bid :
                bids.findByTripIdOrderByAmountAsc(tripId)) {

            if (bid.getStatus() != visibleStatus) {
                continue;
            }

            // Owners see only their own offers.
            if (!admin && !shipper
                    && !actor.getId().equals(bid.getOwnerId())) {
                continue;
            }

            perTruck.putIfAbsent(bid.getTruckId(), bid);
        }

        return perTruck.values()
                .stream()
                .sorted(Comparator.comparing(Bid::getAmount))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public BidResponse acceptBid(String bidId) {

        User actor = current.currentUser();

        Bid selected = bids.findById(bidId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bid", bidId));

        Trip trip = trips.findById(selected.getTripId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip", selected.getTripId()));

        if (!com.jubayer.cholobodlai.Service.MarketplaceRules.canAcceptBid(
                actor.getRole(), actor.getId(), trip.getShipperId())) {

            throw new AccessDeniedException(
                    "You can accept bids only for your own trips");
        }

        if (trip.getStatus() != TripStatus.OPEN
                || selected.getStatus() != BidStatus.PENDING) {

            throw new BusinessRuleException(
                    "Trip or bid has already closed");
        }

        // Reserve this truck atomically.
        Truck reserved = mongo.findAndModify(

                Query.query(
                        Criteria.where("_id")
                                .is(selected.getTruckId())
                                .and("status")
                                .is(TruckStatus.AVAILABLE)
                                .and("verified")
                                .is(true)
                ),

                new Update().set(
                        "status", TruckStatus.ON_TRIP),

                FindAndModifyOptions.options()
                        .returnNew(true),

                Truck.class
        );

        if (reserved == null) {
            throw new BusinessRuleException(
                    "That truck is already assigned to another trip");
        }

        // Assign this trip atomically.
        Trip assigned = mongo.findAndModify(

                Query.query(
                        Criteria.where("_id")
                                .is(trip.getId())
                                .and("status")
                                .is(TripStatus.OPEN)
                ),

                new Update()
                        .set("status", TripStatus.ASSIGNED)
                        .set("assignedTruckId",
                                selected.getTruckId()),

                FindAndModifyOptions.options()
                        .returnNew(true),

                Trip.class
        );

        if (assigned == null) {
            throw new BusinessRuleException(
                    "This trip has already been assigned");
        }

        // The winning offer.
        selected.setStatus(BidStatus.ACCEPTED);
        bids.save(selected);

        // Reject other owners' offers on this trip.
        for (Bid other :
                bids.findByTripIdAndStatus(
                        trip.getId(), BidStatus.PENDING)) {

            if (!other.getId().equals(selected.getId())) {

                other.setStatus(BidStatus.REJECTED);
                bids.save(other);
                users.findById(other.getOwnerId()).ifPresent(u ->
                        notifications.notify(u, "Your offer was not selected for a trip"));
            }
        }

        // Withdraw the winner's bids on other trips.
        for (Bid other :
                bids.findByOwnerIdAndStatus(
                        selected.getOwnerId(), BidStatus.PENDING)) {

            if (!other.getTripId().equals(trip.getId())) {

                other.setStatus(BidStatus.WITHDRAWN);
                bids.save(other);
            }
        }

        users.findById(selected.getOwnerId()).ifPresent(u ->
                notifications.notify(
                        u, "Your offer was accepted! Open My Jobs"));

        return mapper.toResponse(selected);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void withdrawBid(String bidId) {
        Bid bid = bids.findById(bidId).orElseThrow(() ->
                new ResourceNotFoundException("Bid", bidId));
        User actor = current.currentUser();
        if (actor.getRole() != Role.ADMIN && !actor.getId().equals(bid.getOwnerId()))
            throw new AccessDeniedException("You can withdraw only your own offer");
        if (bid.getStatus() != BidStatus.PENDING)
            throw new BusinessRuleException("Only pending offers can be withdrawn");
        bid.setStatus(BidStatus.WITHDRAWN);
        bid.setUpdatedAt(LocalDateTime.now());
        bids.save(bid);
    }

}
