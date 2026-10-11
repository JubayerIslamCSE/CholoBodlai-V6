package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.Model.EmailToken;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Repository.EmailTokenRepository;
import com.jubayer.cholobodlai.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AccountRecoveryService {
    private final UserRepository users;
    private final EmailTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final EmailDeliveryService emails;
    private final SecureRandom random = new SecureRandom();
    private final String baseUrl;
    public AccountRecoveryService(UserRepository users, EmailTokenRepository tokens, PasswordEncoder encoder,
            EmailDeliveryService emails, @Value("${app.public-base-url:http://localhost:8080}") String baseUrl) {
        this.users = users; this.tokens = tokens; this.encoder = encoder; this.emails = emails;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }
    private String newToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
    private void issue(User user, EmailToken.Purpose purpose, int lifetimeMinutes) {
        LocalDateTime now = LocalDateTime.now();
        // Limit requests to one token per purpose per minute. Use a generic public response.
        var outstanding = tokens.findByUserIdAndPurposeAndUsedFalse(user.getId(), purpose);
        if (outstanding.stream().anyMatch(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(now.minusMinutes(1)))) return;
        for (EmailToken previous : outstanding) { previous.setUsed(true); tokens.save(previous); }
        String raw = newToken();
        tokens.save(EmailToken.builder().userId(user.getId()).email(user.getEmail())
                .purpose(purpose).tokenHash(hash(raw)).createdAt(now)
                .expiresAt(now.plusMinutes(lifetimeMinutes)).used(false).build());
        boolean verify = purpose == EmailToken.Purpose.VERIFY_EMAIL;
        String action = baseUrl + (verify ? "/verify-email?token=" : "/reset-password?token=") + raw;
        String content = verify
                ? "Confirm your CholoBodlai email address within 24 hours. Truck owners also require administrator approval."
                : "Reset your CholoBodlai password within 15 minutes. Ignore this message if you did not request it.";
        emails.send(user.getEmail(), verify ? "Confirm your CholoBodlai email" : "Reset CholoBodlai password", content, action);
    }
    public void sendVerification(User user) {
        if (user != null && !user.effectiveEmailVerified() && user.getEmail() != null && !user.getEmail().isBlank())
            issue(user, EmailToken.Purpose.VERIFY_EMAIL, 24 * 60);
    }
    public void resendVerification(String email) {
        if (email == null || email.isBlank() || email.length() > 180) return;
        List<User> matches = users.findByEmailIgnoreCase(email.trim());
        if (matches.size() == 1) sendVerification(matches.get(0));
    }
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank() || email.length() > 180) return;
        List<User> matches = users.findByEmailIgnoreCase(email.trim());
        if (matches.size() == 1 && matches.get(0).effectiveEmailVerified())
            issue(matches.get(0), EmailToken.Purpose.RESET_PASSWORD, 15);
    }
    private Optional<EmailToken> valid(String raw, EmailToken.Purpose purpose) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{35,100}")) return Optional.empty();
        return tokens.findByTokenHashAndUsedFalse(hash(raw))
                .filter(t -> t.getPurpose() == purpose && t.getExpiresAt() != null
                        && t.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(t -> users.findById(t.getUserId())
                        .map(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(t.getEmail()))
                        .orElse(false));
    }
    public boolean validResetToken(String raw) {
        return valid(raw, EmailToken.Purpose.RESET_PASSWORD).isPresent();
    }
    @Transactional
    public boolean verify(String raw) {
        var found = valid(raw, EmailToken.Purpose.VERIFY_EMAIL);
        if (found.isEmpty()) return false;
        var token = found.get();
        User account = users.findById(token.getUserId()).orElseThrow();
        account.setEmailVerified(Boolean.TRUE);
        users.save(account);
        token.setUsed(true); tokens.save(token);
        return true;
    }
    @Transactional
    public void resetPassword(String raw, String password, String confirm) {
        if (password == null || password.length() < 10 || password.length() > 100 || !password.equals(confirm))
            throw new BusinessRuleException("Use a matching password with 10–100 characters");
        EmailToken token = valid(raw, EmailToken.Purpose.RESET_PASSWORD)
                .orElseThrow(() -> new BusinessRuleException("This password reset link expired or was already used"));
        User account = users.findById(token.getUserId()).orElseThrow();
        if (encoder.matches(password, account.getPassword()))
            throw new BusinessRuleException("Choose a password different from your previous password");
        account.setPassword(encoder.encode(password));
        account.setPasswordChangedAt(LocalDateTime.now());
        users.save(account);
        for (EmailToken outstanding : tokens.findByUserIdAndPurposeAndUsedFalse(account.getId(), EmailToken.Purpose.RESET_PASSWORD)) {
            outstanding.setUsed(true); tokens.save(outstanding);
        }
    }
}
