package com.jubayer.cholobodlai.DTO;

public record TruckResponse(
        String id, String registrationNo, String ownerName,
        String truckTypeName, String categoryName, String status, boolean verified
) {}
