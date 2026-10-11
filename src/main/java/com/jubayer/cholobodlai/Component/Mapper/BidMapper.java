package com.jubayer.cholobodlai.Component.Mapper;

import com.jubayer.cholobodlai.DTO.BidResponse;
import com.jubayer.cholobodlai.Model.Bid;
import org.springframework.stereotype.Component;

@Component
public class BidMapper {
    public BidResponse toResponse(Bid b) {
        return new BidResponse(b.getId(), b.getTripId(), b.getOwnerName(),
                b.getTruckRegistrationNo(), b.getAmount(), b.getStatus().name(), b.getCreatedAt());
    }
}