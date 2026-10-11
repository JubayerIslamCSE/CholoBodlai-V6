package com.jubayer.cholobodlai.Security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.util.Date;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final SecretKey key;
    private final long expirationMs;
    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[32]; new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("APP_JWT_SECRET is not configured: using a fresh local-only JWT key. API tokens expire after application restart.");
        } else {
            if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
                throw new IllegalStateException("APP_JWT_SECRET must contain at least 32 UTF-8 bytes");
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expirationMs = expirationMs;
    }
    public String generateToken(UserDetails user) {
        Date now = new Date();
        return Jwts.builder().subject(user.getUsername())
                .claim("role", user.getAuthorities().iterator().next().getAuthority())
                .issuedAt(now).expiration(new Date(now.getTime() + expirationMs))
                .signWith(key).compact();
    }
    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public boolean isValid(String token, UserDetails user) {
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!claims.getSubject().equals(user.getUsername())) return false;
            if (user instanceof AppUserDetails app && app.getPasswordChangedAt() != null) {
                var changedAt = app.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toInstant();
                if (claims.getIssuedAt() == null || claims.getIssuedAt().toInstant().isBefore(changedAt.minusSeconds(1)))
                    return false;
            }
            return true;
        } catch (Exception ignored) { return false; }
    }
}
