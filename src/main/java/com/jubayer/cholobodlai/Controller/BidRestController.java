package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.DTO.BidRequest;
import com.jubayer.cholobodlai.DTO.BidResponse;
import com.jubayer.cholobodlai.Service.Bid.BidService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BidRestController {
    private final BidService bidService;
    public BidRestController(BidService bidService) { this.bidService = bidService; }

    @PostMapping("/bids")
    public ResponseEntity<BidResponse> place(@Valid @RequestBody BidRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bidService.placeBid(req));
    }
    @GetMapping("/trips/{tripId}/bids") public List<BidResponse> forTrip(@PathVariable String tripId) {
        return bidService.getBidsForTrip(tripId);
    }
    @PostMapping("/bids/{bidId}/accept") public BidResponse accept(@PathVariable String bidId) {
        return bidService.acceptBid(bidId);
    }
    @PostMapping("/bids/{id}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String id) {
        bidService.withdrawBid(id);
        return ResponseEntity.noContent().build();
    }
}