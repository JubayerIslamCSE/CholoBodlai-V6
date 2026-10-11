
package com.jubayer.cholobodlai.Repository.Truck;

import com.jubayer.cholobodlai.Model.Emums.TruckStatus;
import com.jubayer.cholobodlai.Model.Truck.Truck;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TruckRepository extends MongoRepository<Truck, String> {

    Optional<Truck> findByRegistrationNo(String registrationNo);

    List<Truck> findByOwnerId(String ownerId);

    List<Truck> findByStatus(TruckStatus status);

    boolean existsByOwnerId(String ownerId);

    boolean existsByRegistrationNo(String registrationNo);
}
