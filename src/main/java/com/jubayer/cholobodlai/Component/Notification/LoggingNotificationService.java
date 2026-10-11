package com.jubayer.cholobodlai.Component.Notification;

import com.jubayer.cholobodlai.Model.Notification;
import com.jubayer.cholobodlai.Model.User;
import com.jubayer.cholobodlai.Repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/** Persistent notifications. No SMS/email gateway is required for local demos. */
@Service
public class LoggingNotificationService implements NotificationService {
    private final NotificationRepository notifications;
    public LoggingNotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }
    @Override
    public void notify(User user, String message) {
        notify(user, message, "/notifications");
    }
    public void notify(User user, String message, String link) {
        if (user == null || user.getId() == null) return;
        notifications.save(Notification.builder().userId(user.getId())
                .message(message).link(link).read(false)
                .createdAt(LocalDateTime.now()).build());
    }
}
