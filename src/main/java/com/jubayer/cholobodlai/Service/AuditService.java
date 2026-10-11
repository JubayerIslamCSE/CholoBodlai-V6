package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.Model.AuditEvent;
import com.jubayer.cholobodlai.Repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditService {
    private final AuditEventRepository events;
    public AuditService(AuditEventRepository events) { this.events = events; }
    public void record(String actorId, String action, String subject) {
        events.save(AuditEvent.builder().actorId(actorId).action(action)
                .subject(subject).happenedAt(LocalDateTime.now()).build());
    }
}
