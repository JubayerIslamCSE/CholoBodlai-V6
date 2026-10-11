package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.DTO.TripRequest;
import com.jubayer.cholobodlai.DTO.TripResponse;
import com.jubayer.cholobodlai.Service.Trip.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripRestController {
    private final TripService tripService;
    public TripRestController(TripService tripService) { this.tripService = tripService; }


    @PostMapping
    public ResponseEntity<TripResponse> create(
            @Valid @RequestBody TripRequest req,
            @RequestParam(required = false) String shipperId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.createFor(req, shipperId));
    }

    @PutMapping("/{id}")
    public TripResponse edit(@PathVariable String id, @Valid @RequestBody TripRequest req) {
        return tripService.update(id, req);
    }
    @PostMapping("/{id}/advance")
    public ResponseEntity<Void> advance(@PathVariable String id) {
        tripService.advance(id); return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable String id) {
        tripService.complete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<TripResponse> list(@RequestParam(defaultValue = "false") boolean openOnly) {
        return openOnly ? tripService.getOpenTrips() : tripService.getAll();
    }
    @GetMapping("/{id}") public TripResponse get(@PathVariable String id) { return tripService.getById(id); }
    @PostMapping("/{id}/cancel") public ResponseEntity<Void> cancel(@PathVariable String id) {
        tripService.cancel(id); return ResponseEntity.noContent().build();
    }
}