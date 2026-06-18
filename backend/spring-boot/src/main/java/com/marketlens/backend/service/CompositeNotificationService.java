package com.marketlens.backend.service;

import com.marketlens.backend.model.NotificationMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompositeNotificationService implements NotificationService {
    private final List<NotificationSender> senders;

    public CompositeNotificationService(List<NotificationSender> senders) {
        this.senders = senders;
    }

    @Override
    public void send(NotificationMessage message) {
        senders.forEach(sender -> sender.send(message));
    }
}
