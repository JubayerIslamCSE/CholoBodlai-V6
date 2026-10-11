package com.jubayer.cholobodlai.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BidRequest(
        @NotBlank String tripId,
        @NotBlank String truckId,
        @NotNull @DecimalMin("0.01") @DecimalMax("50000000.00") @Digits(integer = 8, fraction = 2) BigDecimal amount
) {}