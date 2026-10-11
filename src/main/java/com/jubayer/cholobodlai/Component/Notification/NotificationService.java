package com.jubayer.cholobodlai.Component.Notification;

import com.jubayer.cholobodlai.Model.User;

public interface NotificationService {
    void notify(User recipient, String message);
}