package com.marketlens.backend.model;

import java.time.LocalDateTime;

public record StockMemo(
        Long id,
        String stockCode,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
