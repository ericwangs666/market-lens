package com.marketlens.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StockMemoRequest(
        @NotBlank
        @Size(max = 20)
        @JsonAlias("symbol")
        String stockCode,

        @Size(max = 120)
        @JsonAlias({"name", "stockCode"})
        String title,

        @NotBlank
        @Size(max = 2000)
        @JsonAlias("note")
        String content
) {
}
