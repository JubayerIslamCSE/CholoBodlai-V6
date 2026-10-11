package com.jubayer.cholobodlai.DTO;

public record RegistrationResponse(
        String phone,
        String role,
        String status,
        String message
) {}