package com.jubayer.cholobodlai.Model;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MailPreview {
    private String id;
    private String address;
    private String subject;
    private String message;
    private String actionUrl;
    private LocalDateTime createdAt;
}
