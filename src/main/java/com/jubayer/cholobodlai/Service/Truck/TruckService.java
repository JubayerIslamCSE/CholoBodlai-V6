package com.jubayer.cholobodlai.Service.Truck;

import com.jubayer.cholobodlai.DTO.TruckRequest;
import com.jubayer.cholobodlai.DTO.TruckResponse;
import java.util.List;

public interface TruckService {
    TruckResponse create(TruckRequest request);
    TruckResponse update(String id, TruckRequest request);
    TruckResponse getById(String id);
    List<TruckResponse> getAll();
    void delete(String id);
    TruckResponse verify(String id);
}