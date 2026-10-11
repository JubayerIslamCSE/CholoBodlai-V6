package com.jubayer.cholobodlai.Repository;

import com.jubayer.cholobodlai.Model.AuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {
    List<AuditEvent> findTop30ByOrderByHappenedAtDesc();
}
