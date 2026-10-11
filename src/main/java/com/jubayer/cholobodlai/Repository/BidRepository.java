
package com.jubayer.cholobodlai.Repository;

import com.jubayer.cholobodlai.Model.Bid;
import com.jubayer.cholobodlai.Model.Emums.BidStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface BidRepository extends MongoRepository<Bid, String> {

    List<Bid> findByTripIdOrderByAmountAsc(String tripId);

    List<Bid> findByOwnerId(String ownerId);

    List<Bid> findByTripIdAndTruckId(String tripId, String truckId);

    List<Bid> findByOwnerIdAndStatus(
            String ownerId, BidStatus status);

    List<Bid> findByTripIdAndStatus(
            String tripId, BidStatus status);

    Optional<Bid> findFirstByTripIdAndTruckIdOrderByCreatedAtAsc(
            String tripId, String truckId);
}
