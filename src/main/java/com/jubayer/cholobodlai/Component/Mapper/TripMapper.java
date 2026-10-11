package com.jubayer.cholobodlai.Component.Mapper;

import com.jubayer.cholobodlai.DTO.TripResponse;
import com.jubayer.cholobodlai.Model.Trip;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {
    public TripResponse toResponse(Trip t) {
        return new TripResponse(t.getId(), t.getShipperId(), t.getShipperName(),
                t.getRequestedTypeName(), t.getPickupLocation(), t.getDropoffLocation(),
                t.getDistanceKm(), t.getProductDetails(), t.getLoadingTime(),
                t.isRoundTrip(), t.getEstimatedFare(), t.getStatus().name());
    }
}