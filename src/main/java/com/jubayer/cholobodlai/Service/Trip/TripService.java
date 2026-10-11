
package com.jubayer.cholobodlai.Service.Trip;

import com.jubayer.cholobodlai.DTO.TripRequest;
import com.jubayer.cholobodlai.DTO.TripResponse;
import java.util.List;

public interface TripService {

    TripResponse create(TripRequest request);

    TripResponse createFor(
            TripRequest request, String shipperId);

    TripResponse getById(String id);

    List<TripResponse> getAll();

    List<TripResponse> getOpenTrips();

    TripResponse update(String id, TripRequest request);
    void advance(String id);
    void cancel(String id);

    void complete(String id);
}
