package com.marketlens.backend.service;

import com.marketlens.backend.model.NotificationMessage;

public interface NotificationService {
    void send(NotificationMessage message);
}
