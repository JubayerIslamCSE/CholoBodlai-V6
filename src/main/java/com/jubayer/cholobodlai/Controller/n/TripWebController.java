
package com.jubayer.cholobodlai.Controller.n;

import com.jubayer.cholobodlai.DTO.*;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Model.Bid;
import com.jubayer.cholobodlai.Model.Trip;
import com.jubayer.cholobodlai.Model.Truck.Truck;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import com.jubayer.cholobodlai.Security.AppUserDetails;
import com.jubayer.cholobodlai.Service.*;
import com.jubayer.cholobodlai.Service.Bid.BidService;
import com.jubayer.cholobodlai.Service.Trip.TripService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/trips")
public class TripWebController {

    private final TripService trips;
    private final BidService bids;
    private final LookupService lookups;
    private final BidRepository bidRepository;
    private final TruckRepository truckRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final BookingInteractionService interactions;
    private final PickupTimePolicy pickupTimePolicy;
    private final com.jubayer.cholobodlai.Repository.ReviewRepository reviewRepository;

    public TripWebController(
            TripService trips,
            BidService bids,
            LookupService lookups,
            BidRepository bidRepository,
            TruckRepository truckRepository,
            UserRepository userRepository,
            TripRepository tripRepository,
            com.jubayer.cholobodlai.Repository.ReviewRepository reviewRepository,
            BookingInteractionService interactions,
            PickupTimePolicy pickupTimePolicy) {

        this.trips = trips;
        this.bids = bids;
        this.lookups = lookups;
        this.bidRepository = bidRepository;
        this.truckRepository = truckRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.reviewRepository = reviewRepository;
        this.interactions = interactions;
        this.pickupTimePolicy = pickupTimePolicy;
    }

    @GetMapping
    public String list(
            @AuthenticationPrincipal AppUserDetails principal) {

        return switch (principal.getRole()) {
            case SHIPPER -> "redirect:/shipper";
            case OWNER -> "redirect:/owner";
            case ADMIN -> "redirect:/admin";
        };
    }

    @GetMapping("/new")
    public String newForm(
            Model model,
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(required = false) String shipperId) {

        model.addAttribute(
                "truckTypes", lookups.truckTypes());

        boolean admin = principal.getRole() == Role.ADMIN;

        model.addAttribute("isAdmin", admin);

        model.addAttribute(
                "shippers",
                admin
                        ? userRepository.findByRole(Role.SHIPPER)
                        : List.of());

        model.addAttribute(
                "selectedShipperId", shipperId);
        model.addAttribute("minPickupTime", pickupTimePolicy.earliestAllowedForForm()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));

        return "trips/form";
    }

    @PostMapping
    public String create(
            @RequestParam String truckTypeId,
            @RequestParam String pickupLocation,
            @RequestParam String dropoffLocation,
            @RequestParam Double distanceKm,
            @RequestParam(required = false) String productDetails,
            @RequestParam(required = false) Double cargoWeightKg,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime loadingTime,
            @RequestParam(defaultValue = "false") boolean roundTrip,
            @RequestParam(required = false) String shipperId) {

        TripResponse result = trips.createFor(
                new TripRequest(
                        truckTypeId,
                        pickupLocation,
                        dropoffLocation,
                        distanceKm,
                        productDetails,
                        cargoWeightKg,
                        loadingTime,
                        roundTrip
                ),
                shipperId
        );

        return "redirect:/trips/" + result.id() + "?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model,
                           @AuthenticationPrincipal AppUserDetails principal) {
        trips.getById(id); // service checks access; below verifies ownership for edits
        Trip trip = tripRepository.findById(id).orElseThrow();
        if (trip.getStatus() != TripStatus.OPEN ||
                (principal.getRole() != Role.ADMIN && !trip.getShipperId().equals(principal.getId()))) {
            throw new org.springframework.security.access.AccessDeniedException("Request cannot be edited");
        }
        if (!bidRepository.findByTripIdAndStatus(id, BidStatus.PENDING).isEmpty())
            throw new com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException(
                    "This trip has offers. Cancel and create a new request to change the details");
        model.addAttribute("editingTrip", trip);
        model.addAttribute("minPickupTime", pickupTimePolicy.earliestAllowedForForm()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("loadingTimeValue", trip.getLoadingTime() == null ? "" :
                trip.getLoadingTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("truckTypes", lookups.truckTypes());
        model.addAttribute("isAdmin", principal.getRole() == Role.ADMIN);
        model.addAttribute("shippers", List.of());
        return "trips/edit";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable String id,
            @RequestParam String truckTypeId, @RequestParam String pickupLocation,
            @RequestParam String dropoffLocation, @RequestParam Double distanceKm,
            @RequestParam(required = false) String productDetails,
            @RequestParam(required = false) Double cargoWeightKg,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime loadingTime,
            @RequestParam(defaultValue = "false") boolean roundTrip) {
        trips.update(id, new TripRequest(truckTypeId, pickupLocation, dropoffLocation,
                distanceKm, productDetails, cargoWeightKg, loadingTime, roundTrip));
        return "redirect:/trips/" + id + "?updated";
    }


    @GetMapping("/{id}")
    public String detail(
            @PathVariable String id,
            Model model,
            @AuthenticationPrincipal AppUserDetails principal) {

        // Service checks the user's permission.
        TripResponse summary = trips.getById(id);

        Trip trip = tripRepository.findById(id)
                .orElseThrow();

        boolean admin =
                principal.getRole() == Role.ADMIN;

        boolean shipper =
                principal.getRole() == Role.SHIPPER;

        boolean owner =
                principal.getRole() == Role.OWNER;

        model.addAttribute("trip", summary);
        model.addAttribute("tripRecord", trip);
        model.addAttribute("reviews", reviewRepository.findByTripIdOrderByCreatedAtDesc(id));
        boolean isParticipant = shipper && trip.getShipperId().equals(principal.getId());
        boolean winnerParticipant = owner && bidRepository.findByTripIdAndStatus(id, BidStatus.ACCEPTED)
                .stream().anyMatch(b -> principal.getId().equals(b.getOwnerId()));
        model.addAttribute("canReview", trip.getStatus() == TripStatus.COMPLETED &&
                (isParticipant || winnerParticipant) &&
                !reviewRepository.existsById("review:" + id + ":" + principal.getId()));
        model.addAttribute("canCancel", trip.getStatus() == TripStatus.OPEN && (admin || isParticipant));
        model.addAttribute("canEdit", trip.getStatus() == TripStatus.OPEN && (admin || isParticipant) &&
                bidRepository.findByTripIdAndStatus(id, BidStatus.PENDING).isEmpty());
        model.addAttribute("canAdvance", (admin || winnerParticipant) &&
                (trip.getStatus() == TripStatus.ASSIGNED || trip.getStatus() == TripStatus.PICKUP_READY ||
                        trip.getStatus() == TripStatus.IN_TRANSIT));
        model.addAttribute("advanceText", switch (trip.getStatus()) {
            case ASSIGNED -> "I'm on my way to pickup";
            case PICKUP_READY -> "Cargo picked up — start delivery";
            case IN_TRANSIT -> "Mark cargo delivered";
            default -> "Update delivery";
        });
        model.addAttribute("canTrack", trip.getStatus() != TripStatus.OPEN &&
                trip.getStatus() != TripStatus.CANCELLED);

        model.addAttribute(
                "canAccept",
                (admin || shipper)
                        && trip.getStatus() == TripStatus.OPEN
        );

        model.addAttribute(
                "canComplete",
                (admin || isParticipant)
                        && (trip.getStatus() == TripStatus.DELIVERED ||
                            (admin && (trip.getStatus() == TripStatus.ASSIGNED ||
                                    trip.getStatus() == TripStatus.PICKUP_READY ||
                                    trip.getStatus() == TripStatus.IN_TRANSIT)))
        );

        List<BidResponse> visible =
                bids.getBidsForTrip(id);

        model.addAttribute("bids", visible);

        model.addAttribute(
                "myBid",
                owner && !visible.isEmpty()
                        ? visible.get(0)
                        : null
        );

        List<Truck> matchingTrucks =
                (owner
                        ? lookups.myTrucks()
                        : admin
                          ? truckRepository.findAll()
                          : List.<Truck>of()
                )
                        .stream()
                        .filter(t ->
                                t.getStatus() == TruckStatus.AVAILABLE
                                        && t.isVerified())
                        .filter(t ->
                                Objects.equals(
                                        t.getTruckTypeId(),
                                        trip.getRequestedTypeId()))
                        .toList();

        model.addAttribute(
                "matchingTrucks", matchingTrucks);

        model.addAttribute(
                "mayBid",
                (owner || admin)
                        && trip.getStatus() == TripStatus.OPEN
        );

        Bid accepted =
                bidRepository.findByTripIdAndStatus(
                                id, BidStatus.ACCEPTED)
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (accepted != null
                && (admin || shipper
                || Objects.equals(
                accepted.getOwnerId(),
                principal.getId()))) {

            // Private messages, dispute reports, and receipts are available only
            // to the matched shipper/owner (or administrator).
            var matched = interactions.match(id);
            model.addAttribute("bookingAccess", true);
            model.addAttribute("bookingMessages", interactions.messages(id));
            model.addAttribute("bookingDisputes", interactions.disputesForTrip(id));
            model.addAttribute("winner", accepted);

            model.addAttribute(
                    "winningTruck",
                    truckRepository.findById(
                            accepted.getTruckId()).orElse(null));

            model.addAttribute(
                    "winningOwner",
                    userRepository.findById(
                            accepted.getOwnerId()).orElse(null));

            model.addAttribute(
                    "winningShipper",
                    userRepository.findById(
                            trip.getShipperId()).orElse(null));

            model.addAttribute(
                    "showOwnerContact", admin || shipper);

            model.addAttribute(
                    "showShipperContact", admin || owner);
        }

        return "trips/detail";
    }

    @PostMapping("/{id}/bids")
    public String placeBid(
            @PathVariable String id,
            @RequestParam String truckId,
            @RequestParam BigDecimal amount) {

        bids.placeBid(
                new BidRequest(id, truckId, amount));

        return "redirect:/trips/" + id;
    }

    @GetMapping("/{id}/tracking")
    public String tracking(@PathVariable String id, Model model) {
        trips.getById(id);
        Trip t = tripRepository.findById(id).orElseThrow();
        if (t.getStatus() == TripStatus.OPEN || t.getStatus() == TripStatus.CANCELLED)
            throw new com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException(
                    "Tracking demo is available after a trip is accepted");
        model.addAttribute("trip", t);
        return "trips/tracking";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable String id) {
        trips.cancel(id);
        return "redirect:/shipper?cancelled";
    }

    @PostMapping("/{id}/advance")
    public String advance(@PathVariable String id) {
        trips.advance(id);
        return "redirect:/trips/" + id + "?progress";
    }

    @PostMapping("/{id}/bids/{bidId}/withdraw")
    public String withdraw(@PathVariable String id, @PathVariable String bidId) {
        Bid b = bidRepository.findById(bidId).orElseThrow();
        if (!b.getTripId().equals(id)) throw new IllegalArgumentException("Mismatched offer");
        bids.withdrawBid(bidId);
        return "redirect:/owner?withdrawn";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable String id) {

        trips.complete(id);

        return "redirect:/trips/" + id;
    }

    @PostMapping("/{id}/bids/{bidId}/accept")
    public String acceptBid(
            @PathVariable String id,
            @PathVariable String bidId) {

        Bid selected = bidRepository.findById(bidId)
                .orElseThrow();

        if (!id.equals(selected.getTripId())) {
            throw new IllegalArgumentException(
                    "Bid is for another trip");
        }

        bids.acceptBid(bidId);

        return "redirect:/trips/" + id;
    }
}
