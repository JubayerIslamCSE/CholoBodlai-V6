package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.DTO.TruckRequest;
import com.jubayer.cholobodlai.DTO.TruckResponse;
import com.jubayer.cholobodlai.Service.Truck.TruckService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/trucks")
public class TruckRestController {
    private final TruckService truckService;
    public TruckRestController(TruckService truckService) { this.truckService = truckService; }

    @PostMapping
    public ResponseEntity<TruckResponse> create(@Valid @RequestBody TruckRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(truckService.create(req));
    }
    @GetMapping
    public List<TruckResponse> list() { return truckService.getAll(); }
    @GetMapping("/{id}") public TruckResponse get(@PathVariable String id) { return truckService.getById(id); }
    @PutMapping("/{id}") public TruckResponse update(@PathVariable String id, @Valid @RequestBody TruckRequest req) {
        return truckService.update(id, req);
    }
    @PostMapping("/{id}/verify") public TruckResponse verify(@PathVariable String id) { return truckService.verify(id); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id) {
        truckService.delete(id); return ResponseEntity.noContent().build();
    }
}