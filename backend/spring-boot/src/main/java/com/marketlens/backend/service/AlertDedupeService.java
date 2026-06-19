package com.marketlens.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
public class AlertDedupeService {
    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public AlertDedupeService(
            StringRedisTemplate redisTemplate,
            @Value("${market-lens.alerts.dedupe-ttl-hours:24}") long ttlHours
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofHours(ttlHours);
    }

    public boolean markIfFirstTrigger(Long ruleId, LocalDate eventDate) {
        String key = "market-lens:alert-dedupe:" + ruleId + ":" + eventDate;
        Boolean stored = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(stored);
    }
}
