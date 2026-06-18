package com.marketlens.backend.model;

public record StockSearchResult(
        String code,
        String name,
        String market,
        String type
) {
}
