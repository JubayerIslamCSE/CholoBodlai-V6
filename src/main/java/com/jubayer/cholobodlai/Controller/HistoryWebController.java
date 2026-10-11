package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Model.Emums.*;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Service.CurrentUserService;
import com.jubayer.cholobodlai.Service.InputPolicy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
@RequestMapping("/history")
public class HistoryWebController {
    private final CurrentUserService current;
    private final TripRepository trips;
    private final BidRepository bids;
    public record Row(Trip trip, Bid bid) {}
    public HistoryWebController(CurrentUserService current, TripRepository trips, BidRepository bids) {
        this.current = current; this.trips = trips; this.bids = bids;
    }
    @GetMapping
    public String history(@RequestParam(required = false) String query,
                          @RequestParam(required = false) String status, Model model) {
        User user = current.currentUser();
        List<Row> all = new ArrayList<>();
        if (user.getRole() == Role.OWNER) {
            Map<String, Row> byTrip = new LinkedHashMap<>();
            for (Bid bid : bids.findByOwnerId(user.getId())) {
                trips.findById(bid.getTripId()).ifPresent(t -> {
                    Row previous = byTrip.get(t.getId());
                    if (previous == null || previous.bid().getStatus() != BidStatus.ACCEPTED)
                        byTrip.put(t.getId(), new Row(t, bid));
                });
            }
            all.addAll(byTrip.values());
        } else {
            List<Trip> visible = user.getRole() == Role.ADMIN
                    ? trips.findAll() : trips.findByShipperId(user.getId());
            for (Trip trip : visible) all.add(new Row(trip, null));
        }
        String text = InputPolicy.searchTerm(query);
        String choice = InputPolicy.statusTerm(status, Set.of("OPEN","ASSIGNED","PICKUP_READY","IN_TRANSIT","DELIVERED","COMPLETED","CANCELLED","PENDING","ACCEPTED","REJECTED","WITHDRAWN"));
        List<Row> visible = all.stream()
                .filter(x -> text.isBlank() || (
                        Objects.toString(x.trip().getPickupLocation(), "").toLowerCase(Locale.ROOT).contains(text)
                        || Objects.toString(x.trip().getDropoffLocation(), "").toLowerCase(Locale.ROOT).contains(text)))
                .filter(x -> choice.isBlank() || choice.equals("ALL")
                        || x.trip().getStatus().name().equals(choice)
                        || (x.bid() != null && x.bid().getStatus().name().equals(choice)))
                .sorted(Comparator.comparing((Row x) -> x.trip().getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        model.addAttribute("entries", visible);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("selectedStatus", choice);
        model.addAttribute("ownerHistory", user.getRole() == Role.OWNER);
        model.addAttribute("filterStatuses", List.of("OPEN","ASSIGNED","PICKUP_READY","IN_TRANSIT","DELIVERED","COMPLETED","CANCELLED","PENDING","ACCEPTED","REJECTED","WITHDRAWN"));
        return "history";
    }
}
