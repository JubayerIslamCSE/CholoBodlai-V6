package com.jubayer.cholobodlai.Model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "account_email_tokens")
public class EmailToken {
    public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }
    @Id private String id;
    private String userId;
    private String email;
    private Purpose purpose;
    private String tokenHash;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean used;
}
