package com.jubayer.cholobodlai.Security;

import com.jubayer.cholobodlai.Model.*;
import com.jubayer.cholobodlai.Repository.*;
import com.jubayer.cholobodlai.Service.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AccountRecoveryServiceTest {
    @Test void unknownEmailDoesNotCreateToken() {
        UserRepository users=mock(UserRepository.class);
        EmailTokenRepository tokens=mock(EmailTokenRepository.class);
        EmailDeliveryService mail=mock(EmailDeliveryService.class);
        PasswordEncoder encoder=mock(PasswordEncoder.class);
        when(users.findByEmailIgnoreCase("unknown@example.com")).thenReturn(List.of());
        var service=new AccountRecoveryService(users,tokens,encoder,mail,"http://localhost:8080");
        service.requestPasswordReset("unknown@example.com");
        verifyNoInteractions(tokens,mail);
    }
    @Test void verificationTokenEnablesOnlyEmailNotOwnerApproval() {
        UserRepository users=mock(UserRepository.class);
        EmailTokenRepository tokens=mock(EmailTokenRepository.class);
        EmailDeliveryService mail=mock(EmailDeliveryService.class);
        PasswordEncoder encoder=mock(PasswordEncoder.class);
        var u=User.builder().id("id").email("owner@example.com")
                .role(com.jubayer.cholobodlai.Model.Emums.Role.OWNER)
                .approvalStatus(com.jubayer.cholobodlai.Model.Emums.ApprovalStatus.PENDING)
                .emailVerified(false).enabled(false).build();
        var token=EmailToken.builder().userId("id").email("owner@example.com")
                .purpose(EmailToken.Purpose.VERIFY_EMAIL).used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(20)).build();
        String raw="A".repeat(43);
        when(tokens.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.of(token));
        when(users.findById("id")).thenReturn(Optional.of(u));
        var service=new AccountRecoveryService(users,tokens,encoder,mail,"http://localhost:8080");
        assertTrue(service.verify(raw));
        assertTrue(u.effectiveEmailVerified());
        assertFalse(new AppUserDetails(u).isEnabled());
        assertTrue(token.isUsed());
        verify(users).save(u);
    }
    @Test void previouslyUsedResetTokenIsRejected() {
        UserRepository users=mock(UserRepository.class);
        EmailTokenRepository tokens=mock(EmailTokenRepository.class);
        EmailDeliveryService mail=mock(EmailDeliveryService.class);
        PasswordEncoder encoder=mock(PasswordEncoder.class);
        when(tokens.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.empty());
        var service=new AccountRecoveryService(users,tokens,encoder,mail,"http://localhost:8080");
        assertFalse(service.validResetToken("A".repeat(43)));
    }
}
