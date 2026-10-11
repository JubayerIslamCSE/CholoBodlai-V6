package com.jubayer.cholobodlai.Repository;

import com.jubayer.cholobodlai.Model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndReadFalse(String userId);
}
