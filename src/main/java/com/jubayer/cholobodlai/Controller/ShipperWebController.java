
package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.EXCEPTION.*;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.Trip;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Service.CurrentUserService;
import com.jubayer.cholobodlai.Service.InputPolicy;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
public class ShipperWebController {

    private final TripRepository trips;
    private final UserRepository users;
    private final CurrentUserService current;

    public ShipperWebController(
            TripRepository trips,
            UserRepository users,
            CurrentUserService current) {

        this.trips = trips;
        this.users = users;
        this.current = current;
    }

    @GetMapping
    public String dashboard(
            Model model,
            @RequestParam(required = false) String shipperId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        User actor = current.currentUser();
        boolean admin = actor.getRole() == Role.ADMIN;

        List<Trip> visible;

        if (!admin) {

            visible = trips.findByShipperId(actor.getId());

        } else if (shipperId != null
                && !shipperId.isBlank()) {

            User shipper = users.findById(shipperId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User", shipperId));

            if (shipper.getRole() != Role.SHIPPER) {
                throw new BusinessRuleException(
                        "Choose a shipper account");
            }

            visible = trips.findByShipperId(shipperId);

        } else {

            visible = trips.findAll();
        }

        long pendingCount = visible.stream().filter(t -> t.getStatus() ==
                com.jubayer.cholobodlai.Model.Emums.TripStatus.OPEN).count();
        long activeCount = visible.stream().filter(t -> java.util.EnumSet.of(
                com.jubayer.cholobodlai.Model.Emums.TripStatus.ASSIGNED,
                com.jubayer.cholobodlai.Model.Emums.TripStatus.PICKUP_READY,
                com.jubayer.cholobodlai.Model.Emums.TripStatus.IN_TRANSIT,
                com.jubayer.cholobodlai.Model.Emums.TripStatus.DELIVERED)
                .contains(t.getStatus())).count();
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("activeCount", activeCount);
        String filter = InputPolicy.searchTerm(search);
        String state = InputPolicy.statusTerm(status, java.util.Set.of("OPEN","ASSIGNED","PICKUP_READY","IN_TRANSIT","DELIVERED","COMPLETED","CANCELLED"));
        List<Trip> filtered = visible.stream()
                .filter(t -> filter.isEmpty() || (t.getPickupLocation() != null && t.getPickupLocation().toLowerCase(java.util.Locale.ROOT).contains(filter))
                        || (t.getDropoffLocation() != null && t.getDropoffLocation().toLowerCase(java.util.Locale.ROOT).contains(filter)))
                .filter(t -> state.isEmpty() || state.equals("ALL") || t.getStatus().name().equals(state))
                .toList();
        model.addAttribute("trips", filtered);
        model.addAttribute("filterSearch", search == null ? "" : search);
        model.addAttribute("filterStatus", state);
        model.addAttribute("statusFilters", java.util.List.of("OPEN","ASSIGNED","PICKUP_READY","IN_TRANSIT","DELIVERED","COMPLETED","CANCELLED"));
        model.addAttribute("isAdmin", admin);

        model.addAttribute(
                "shippers",
                admin
                        ? users.findByRole(Role.SHIPPER)
                        : List.of());

        model.addAttribute(
                "selectedShipperId", shipperId);

        return "shipper/dashboard";
    }
}
