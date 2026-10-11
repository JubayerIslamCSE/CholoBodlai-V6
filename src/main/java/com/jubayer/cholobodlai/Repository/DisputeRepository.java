package com.jubayer.cholobodlai.Repository;
import com.jubayer.cholobodlai.Model.Dispute;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface DisputeRepository extends MongoRepository<Dispute,String> {
    List<Dispute> findByTripIdOrderByCreatedAtDesc(String tripId);
    List<Dispute> findAllByOrderByCreatedAtDesc();
    boolean existsByTripIdAndOpenedByIdAndStatus(String tripId, String openedById, Dispute.Status status);
}
