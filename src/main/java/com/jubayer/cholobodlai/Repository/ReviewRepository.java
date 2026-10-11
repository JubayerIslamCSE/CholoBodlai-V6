package com.jubayer.cholobodlai.Repository;

import com.jubayer.cholobodlai.Model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByTripIdOrderByCreatedAtDesc(String tripId);
    List<Review> findBySubjectId(String subjectId);
}
