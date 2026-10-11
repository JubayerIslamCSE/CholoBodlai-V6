package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.Service.BookingInteractionService;
import com.jubayer.cholobodlai.Repository.Truck.TruckRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class BookingInteractionWebController {
    private final BookingInteractionService interactions;
    private final TruckRepository trucks;
    public BookingInteractionWebController(BookingInteractionService interactions, TruckRepository trucks) {
        this.interactions = interactions; this.trucks = trucks;
    }
    @PostMapping("/trips/{id}/messages")
    public String sendMessage(@PathVariable String id, @RequestParam String message) {
        interactions.sendMessage(id, message);
        return "redirect:/trips/" + id + "?messageSent";
    }
    @PostMapping("/trips/{id}/disputes")
    public String report(@PathVariable String id, @RequestParam String reason) {
        interactions.openDispute(id, reason);
        return "redirect:/trips/" + id + "?reported";
    }
    @GetMapping("/trips/{id}/receipt")
    public String receipt(@PathVariable String id, Model model) {
        var booking = interactions.match(id);
        model.addAttribute("trip", booking.trip());
        model.addAttribute("acceptedBid", booking.accepted());
        model.addAttribute("shipper", booking.shipper());
        model.addAttribute("owner", booking.owner());
        model.addAttribute("truck", trucks.findById(booking.accepted().getTruckId()).orElse(null));
        return "trips/receipt";
    }
    @PostMapping("/admin/disputes/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public String resolve(@PathVariable String id, @RequestParam String response) {
        interactions.resolveDispute(id, response);
        return "redirect:/admin?disputeResolved#disputes";
    }
}
