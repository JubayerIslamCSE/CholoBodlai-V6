package com.jubayer.cholobodlai.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "audit_events")
public class AuditEvent {
    @Id private String id;
    private String actorId;
    private String action;
    private String subject;
    private LocalDateTime happenedAt;
}
