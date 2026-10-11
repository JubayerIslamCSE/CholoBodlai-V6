package com.jubayer.cholobodlai.Model;

import com.jubayer.cholobodlai.Model.Emums.ApprovalStatus;
import com.jubayer.cholobodlai.Model.Emums.Role;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String fullName;
    @Indexed(unique = true)
    private String phone;
    private String email;
    // null means a pre-existing account; preserve legacy access until its next email change.
    private Boolean emailVerified;
    private String password;   // BCrypt hash
    private Role role;
    private boolean enabled;

    private ApprovalStatus approvalStatus;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;
    private LocalDateTime passwordChangedAt;

    public boolean effectiveEmailVerified() { return emailVerified == null || emailVerified; }

    // Compatibility with users already stored in Atlas.
    public ApprovalStatus effectiveApprovalStatus() {
        if (approvalStatus != null) {
            return approvalStatus;
        }

        return enabled
                ? ApprovalStatus.APPROVED
                : ApprovalStatus.PENDING;
    }

}
