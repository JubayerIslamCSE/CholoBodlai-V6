package com.jubayer.cholobodlai.Security;

import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Model.Emums.ApprovalStatus;
import com.jubayer.cholobodlai.Model.Emums.Role;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private AppUserDetails user(LocalDateTime passwordChangedAt) {
        return new AppUserDetails(User.builder().id("test-user").fullName("Test User")
                .phone("01700000000").password("not-a-real-hash")
                .enabled(true).approvalStatus(ApprovalStatus.APPROVED)
                .role(Role.SHIPPER).passwordChangedAt(passwordChangedAt).build());
    }
    @Test void newlyIssuedApiTokenIsValid() {
        JwtService jwt = new JwtService("", 600000);
        AppUserDetails user = user(null);
        String token = jwt.generateToken(user);
        assertEquals(user.getUsername(), jwt.extractUsername(token));
        assertTrue(jwt.isValid(token, user));
    }
    @Test void tokenInvalidatesAfterPasswordChange() {
        JwtService jwt = new JwtService("", 600000);
        String token = jwt.generateToken(user(null));
        assertFalse(jwt.isValid(token, user(LocalDateTime.now().plusMinutes(2))));
    }
    @Test void tokensUsePerInstanceKeysWhenUnconfigured() {
        JwtService first = new JwtService("", 600000);
        JwtService afterRestart = new JwtService("", 600000);
        AppUserDetails user = user(null);
        assertFalse(afterRestart.isValid(first.generateToken(user), user));
    }
}
