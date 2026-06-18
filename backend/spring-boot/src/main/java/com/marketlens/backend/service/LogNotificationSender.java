package com.marketlens.backend.service;

import com.marketlens.backend.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(NotificationMessage message) {
        log.info("Alert notification: {} - {}", message.title(), message.body());
    }
}
