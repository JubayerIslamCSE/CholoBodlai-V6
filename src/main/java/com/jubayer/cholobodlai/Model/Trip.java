package com.jubayer.cholobodlai.Model;

import com.jubayer.cholobodlai.Model.Emums.TripStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "trips")
public class Trip {
    @Id
    private String id;
    private String shipperId;          // reference
    private String shipperName;        // snapshot
    private String requestedTypeId;    // reference
    private String requestedTypeName;  // snapshot
    private BigDecimal baseRatePerKm;  // snapshot — the pricing engine reads this
    private String pickupLocation;
    private String dropoffLocation;
    private Double distanceKm;
    private String productDetails;
    private LocalDateTime loadingTime;
    private boolean roundTrip;
    private BigDecimal estimatedFare;
    private TripStatus status;
    private String assignedTruckId;    // nullable
    private Double cargoWeightKg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;

}
