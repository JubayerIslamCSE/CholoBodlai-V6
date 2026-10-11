package com.jubayer.cholobodlai.Repository.Truck;

import com.jubayer.cholobodlai.Model.Truck.TruckType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TruckTypeRepository extends MongoRepository<TruckType, String> {
    List<TruckType> findByCategoryId(String categoryId);
}
