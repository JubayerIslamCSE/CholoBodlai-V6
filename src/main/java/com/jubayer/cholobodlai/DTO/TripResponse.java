package com.jubayer.cholobodlai.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripResponse(
        String id, String shipperId, String shipperName, String requestedTypeName,
        String pickupLocation, String dropoffLocation, Double distanceKm,
        String productDetails, LocalDateTime loadingTime, boolean roundTrip,
        BigDecimal estimatedFare, String status
) {}