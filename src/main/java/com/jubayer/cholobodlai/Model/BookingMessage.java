package com.jubayer.cholobodlai.Model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "booking_messages")
public class BookingMessage {
    @Id private String id;
    @Indexed private String tripId;
    private String senderId;
    private String senderName;
    private String body;
    private LocalDateTime createdAt;
}
