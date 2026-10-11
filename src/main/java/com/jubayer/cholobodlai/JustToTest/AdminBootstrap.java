
package com.jubayer.cholobodlai.JustToTest;

import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Model.Emums.ApprovalStatus;
import com.jubayer.cholobodlai.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(20)
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${APP_ADMIN_PHONE:}")
    private String phone;

    @Value("${APP_ADMIN_PASSWORD:}")
    private String password;

    public AdminBootstrap(
            UserRepository users,
            PasswordEncoder encoder) {

        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {

        if (phone.isBlank() && password.isBlank()) {
            return;
        }

        if (phone.isBlank() || password.length() < 10) {
            throw new IllegalStateException(
                    "Set APP_ADMIN_PHONE and a strong APP_ADMIN_PASSWORD"
            );
        }

        String adminPhone = phone.trim();

        var existing = users.findByPhone(adminPhone);

        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN) {
                throw new IllegalStateException(
                        "This phone belongs to a non-admin account"
                );
            }

            // Don't overwrite an existing admin password.
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        User admin = User.builder()
                .fullName("CholoBodlai Admin")
                .phone(adminPhone)
                .email("")
                .password(encoder.encode(password))
                .role(Role.ADMIN)
                .enabled(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .createdAt(now)
                .approvedAt(now)
                .build();

        users.insert(admin);
    }
}
