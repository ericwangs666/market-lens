package com.marketlens.backend.service;

import com.marketlens.backend.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TelegramNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationSender.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String botToken;
    private final String chatId;

    public TelegramNotificationSender(
            RestClient.Builder restClientBuilder,
            @Value("${market-lens.notifications.telegram-enabled:false}") boolean enabled,
            @Value("${market-lens.notifications.telegram-bot-token:}") String botToken,
            @Value("${market-lens.notifications.telegram-chat-id:}") String chatId
    ) {
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
        this.botToken = botToken;
        this.chatId = chatId;
    }

    @Override
    public void send(NotificationMessage message) {
        if (!enabled || !StringUtils.hasText(botToken) || !StringUtils.hasText(chatId)) {
            return;
        }

        try {
            restClient.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", message.title() + "\n" + message.body()
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("Failed to send Telegram notification", exception);
        }
    }
}
