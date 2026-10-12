package com.jubayer.cholobodlai.Service;

import com.jubayer.cholobodlai.Model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Controls whether email verification is mandatory.
 *
 * Admin approval and account suspension remain mandatory
 * regardless of this setting.
 *
 * If verification is enabled again, accounts with unverified
 * email addresses will need verification before signing in.
 */
@Component
public class EmailVerificationPolicy {

    private final boolean required;

    public EmailVerificationPolicy(
            @Value("${app.email.verification-required:true}")
            boolean required) {

        this.required = required;
    }

    public boolean required() {
        return required;
    }

    public boolean permitted(User user) {
        return !required || user.effectiveEmailVerified();
    }
}
