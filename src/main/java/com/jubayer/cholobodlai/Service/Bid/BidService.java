package com.jubayer.cholobodlai.Service.Bid;

import com.jubayer.cholobodlai.DTO.BidRequest;
import com.jubayer.cholobodlai.DTO.BidResponse;
import java.util.List;

public interface BidService {
    BidResponse placeBid(BidRequest request);
    List<BidResponse> getBidsForTrip(String tripId);
    BidResponse acceptBid(String bidId);
    void withdrawBid(String bidId);
}