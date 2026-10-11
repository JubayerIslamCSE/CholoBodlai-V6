package com.jubayer.cholobodlai.Repository;

import com.jubayer.cholobodlai.Model.Emums.TripStatus;
import com.jubayer.cholobodlai.Model.Trip;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TripRepository extends MongoRepository<Trip, String> {
    List<Trip> findByStatus(TripStatus status);
    List<Trip> findByShipperId(String shipperId);
    long countByStatus(TripStatus status);

}
