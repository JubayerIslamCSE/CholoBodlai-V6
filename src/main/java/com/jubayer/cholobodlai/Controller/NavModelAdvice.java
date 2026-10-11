package com.jubayer.cholobodlai.Controller;

import com.jubayer.cholobodlai.Repository.NotificationRepository;
import com.jubayer.cholobodlai.Service.EmailDeliveryService;
import com.jubayer.cholobodlai.Security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class NavModelAdvice {
    private final NotificationRepository notifications;
    private final EmailDeliveryService mail;
    public NavModelAdvice(NotificationRepository notifications, EmailDeliveryService mail) {
        this.notifications = notifications; this.mail = mail;
    }
    @ModelAttribute("demoMailPreview")
    public boolean preview() { return mail.previewEnabled(); }
    @ModelAttribute("unreadCount")
    public long unread(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails a)) return 0;
        return notifications.countByUserIdAndReadFalse(a.getId());
    }
}
