package com.marketlens.backend.service;

import com.marketlens.backend.model.NotificationMessage;

public interface NotificationSender {
    void send(NotificationMessage message);
}
