
package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.Repository.NotificationRepository;
import com.jubayer.cholobodlai.Service.EmailDeliveryService;
import com.jubayer.cholobodlai.Service.EmailVerificationPolicy;
import com.jubayer.cholobodlai.Security.AppUserDetails;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class NavModelAdvice {

    private final NotificationRepository notifications;
    private final EmailDeliveryService mail;
    private final EmailVerificationPolicy emailPolicy;

    public NavModelAdvice(
            NotificationRepository notifications,
            EmailDeliveryService mail,
            EmailVerificationPolicy emailPolicy) {

        this.notifications = notifications;
        this.mail = mail;
        this.emailPolicy = emailPolicy;
    }

    @ModelAttribute("emailVerificationRequired")
    public boolean emailVerificationRequired() {
        return emailPolicy.required();
    }

    @ModelAttribute("mailDeliveryReady")
    public boolean mailDeliveryReady() {
        return mail.deliveryReady();
    }

    @ModelAttribute("demoMailPreview")
    public boolean preview() {
        return mail.previewEnabled();
    }

    @ModelAttribute("unreadCount")
    public long unread(Authentication authentication) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AppUserDetails user)) {
            return 0;
        }

        return notifications.countByUserIdAndReadFalse(
                user.getId()
        );
    }
}