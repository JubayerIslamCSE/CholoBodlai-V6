package com.jubayer.cholobodlai.DTO;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String phone,
        @NotBlank String password
) {}