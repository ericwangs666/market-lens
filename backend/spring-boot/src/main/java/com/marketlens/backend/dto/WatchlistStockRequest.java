package com.marketlens.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WatchlistStockRequest(
        @NotBlank
        @Size(max = 20)
        @JsonAlias("symbol")
        String code,

        @Size(max = 80)
        @JsonAlias("symbol")
        String name,

        @NotBlank
        @Pattern(regexp = "(?i)A|CN|US|HK|ETF", message = "must be one of A, CN, US, HK, ETF")
        String market
) {
}
