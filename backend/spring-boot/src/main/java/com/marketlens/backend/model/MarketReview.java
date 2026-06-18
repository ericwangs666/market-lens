package com.marketlens.backend.model;

import java.time.LocalDate;
import java.util.List;

public record MarketReview(
        LocalDate reviewDate,
        String title,
        List<String> highlights,
        List<String> leadingStocks,
        String source
) {
}
