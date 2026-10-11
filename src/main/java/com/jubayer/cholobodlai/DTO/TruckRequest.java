package com.jubayer.cholobodlai.DTO;

import jakarta.validation.constraints.NotBlank;

public record TruckRequest(
        @NotBlank String registrationNo,
        @NotBlank String truckTypeId
) {}