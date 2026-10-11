package com.jubayer.cholobodlai.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "reviews")
public class Review {
    @Id private String id;
    private String tripId;
    private String authorId;
    private String subjectId;
    private String authorName;
    private int stars;
    private String comment;
    private LocalDateTime createdAt;
}
