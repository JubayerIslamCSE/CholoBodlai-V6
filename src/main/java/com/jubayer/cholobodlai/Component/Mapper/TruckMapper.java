package com.jubayer.cholobodlai.Component.Mapper;

import com.jubayer.cholobodlai.DTO.TruckResponse;
import com.jubayer.cholobodlai.Model.Truck.Truck;
import org.springframework.stereotype.Component;

@Component
public class TruckMapper {
    public TruckResponse toResponse(Truck t) {
        return new TruckResponse(t.getId(), t.getRegistrationNo(), t.getOwnerName(),
                t.getTruckTypeName(), t.getCategoryName(), t.getStatus().name(), t.isVerified());
    }
}