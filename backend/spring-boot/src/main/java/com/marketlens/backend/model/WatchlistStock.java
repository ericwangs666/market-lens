package com.marketlens.backend.model;

import java.time.LocalDateTime;

public record WatchlistStock(
        Long id,
        String code,
        String name,
        String market,
        LocalDateTime createdAt
) {
}
