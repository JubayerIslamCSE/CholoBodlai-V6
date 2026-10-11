package com.jubayer.cholobodlai.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidResponse(
        String id, String tripId, String ownerName, String truckRegistrationNo,
        BigDecimal amount, String status, LocalDateTime createdAt
) {}