package com.jubayer.cholobodlai.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record TripRequest(
        @NotBlank String truckTypeId,
        @NotBlank @Size(max = 180) String pickupLocation,
        @NotBlank @Size(max = 180) String dropoffLocation,
        @NotNull @DecimalMin("0.1") @DecimalMax("10000.0") Double distanceKm,
        @Size(max = 1500) String productDetails,
        @DecimalMin("0.1") @DecimalMax("50000.0") Double cargoWeightKg,
        @NotNull(message = "Pickup date and time are required") LocalDateTime loadingTime,
        boolean roundTrip
) {
    // Compatible with the previous seven-argument controller/client constructor.
    public TripRequest(String type, String from, String to, Double km,
                       String goods, LocalDateTime time, boolean round) {
        this(type, from, to, km, goods, null, time, round);
    }
}