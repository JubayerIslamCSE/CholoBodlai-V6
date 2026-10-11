
package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.EXCEPTION.*;
import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Model.Truck.Truck;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import com.jubayer.cholobodlai.Service.CurrentUserService;
import com.jubayer.cholobodlai.Service.InputPolicy;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class OwnerWebController {

    private final CurrentUserService current;
    private final UserRepository users;
    private final TruckRepository trucks;
    private final TripRepository trips;
    private final BidRepository bids;

    public record WonJob(
            Trip trip, Bid bid, User shipper) {}

    public record ActiveOffer(
            Trip trip, Bid bid) {}

    public OwnerWebController(
            CurrentUserService current,
            UserRepository users,
            TruckRepository trucks,
            TripRepository trips,
            BidRepository bids) {

        this.current = current;
        this.users = users;
        this.trucks = trucks;
        this.trips = trips;
        this.bids = bids;
    }

    @GetMapping
    public String dashboard(
            Model model,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String pickup,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String truckType) {

        User actor = current.currentUser();
        boolean admin = actor.getRole() == Role.ADMIN;

        String selected = actor.getId();

        if (admin) {

            selected = ownerId;

            if (selected != null && !selected.isBlank()) {

                User requestedOwner =
                        users.findById(selected)
                                .orElseThrow(() ->
                                        new ResourceNotFoundException(
                                                "User", ownerId));

                if (requestedOwner.getRole() != Role.OWNER) {
                    throw new BusinessRuleException(
                            "Select an owner account");
                }
            }
        }

        List<Bid> won =
                admin && (selected == null || selected.isBlank())
                        ? bids.findAll().stream()
                        .filter(b ->
                                b.getStatus() == BidStatus.ACCEPTED)
                        .toList()

                        : bids.findByOwnerIdAndStatus(
                        selected, BidStatus.ACCEPTED);

        List<WonJob> jobs = won.stream()
                .flatMap(b ->
                        trips.findById(b.getTripId()).stream()
                                .flatMap(t ->
                                        users.findById(t.getShipperId())
                                                .stream()
                                                .map(s ->
                                                        new WonJob(t, b, s))))
                .toList();

        List<Bid> active =
                admin && (selected == null || selected.isBlank())
                        ? bids.findAll().stream()
                        .filter(b ->
                                b.getStatus() == BidStatus.PENDING)
                        .toList()

                        : bids.findByOwnerIdAndStatus(
                        selected, BidStatus.PENDING);

        List<Truck> visibleTrucks =
                admin && (selected == null || selected.isBlank())
                        ? trucks.findAll()
                        : trucks.findByOwnerId(selected);

        boolean busy =
                !admin && visibleTrucks.stream()
                        .anyMatch(t ->
                                t.getStatus() == TruckStatus.ON_TRIP);

        List<ActiveOffer> offers =
                (busy ? List.<Bid>of() : active).stream()
                        .flatMap(b ->
                                trips.findById(b.getTripId())
                                        .stream()
                                        .map(t -> new ActiveOffer(t, b)))
                        .toList();

        String pickupSearch = InputPolicy.searchTerm(pickup);
        String typeSearch = InputPolicy.searchTerm(truckType);
        String destinationSearch = InputPolicy.searchTerm(destination);
        List<Trip> available = busy ? List.of() : trips.findByStatus(TripStatus.OPEN).stream()
                .filter(t -> pickupSearch.isEmpty() || (t.getPickupLocation() != null &&
                        t.getPickupLocation().toLowerCase(java.util.Locale.ROOT).contains(pickupSearch)))
                .filter(t -> destinationSearch.isEmpty() || (t.getDropoffLocation() != null &&
                        t.getDropoffLocation().toLowerCase(java.util.Locale.ROOT).contains(destinationSearch)))
                .filter(t -> typeSearch.isEmpty() || (t.getRequestedTypeName() != null &&
                        t.getRequestedTypeName().toLowerCase(java.util.Locale.ROOT).contains(typeSearch)))
                .toList();
        model.addAttribute("openTrips", available);
        model.addAttribute("pickupFilter", pickup == null ? "" : pickup);
        model.addAttribute("typeFilter", truckType == null ? "" : truckType);
        model.addAttribute("destinationFilter", destination == null ? "" : destination);

        model.addAttribute("myTrucks", visibleTrucks);
        model.addAttribute("activeBids", offers);
        model.addAttribute("wonJobs", jobs);
        model.addAttribute("isAdmin", admin);

        model.addAttribute(
                "owners",
                admin
                        ? users.findByRole(Role.OWNER)
                        : List.of());

        model.addAttribute("selectedOwnerId", ownerId);

        return "owner/dashboard";
    }
}
