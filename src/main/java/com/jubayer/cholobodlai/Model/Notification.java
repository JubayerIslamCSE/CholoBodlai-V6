package com.jubayer.cholobodlai.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "notifications")
public class Notification {
    @Id private String id;
    private String userId;
    private String message;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;
}
