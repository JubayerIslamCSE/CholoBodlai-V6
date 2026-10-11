package com.jubayer.cholobodlai.Repository.Truck;

import com.jubayer.cholobodlai.Model.Truck.TruckCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TruckCategoryRepository extends MongoRepository<TruckCategory, String> {
    Optional<TruckCategory> findByName(String name);
}
