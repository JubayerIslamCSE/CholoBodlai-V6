package com.jubayer.cholobodlai.Model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "booking_disputes")
public class Dispute {
    public enum Status { OPEN, RESOLVED }
    @Id private String id;
    @Indexed private String tripId;
    private String openedById;
    private String openedByName;
    private String description;
    private Status status;
    private String adminResponse;
    private String reviewedById;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
