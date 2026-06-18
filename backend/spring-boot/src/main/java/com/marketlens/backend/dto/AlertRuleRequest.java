package com.marketlens.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AlertRuleRequest(
        @NotBlank
        @Size(max = 20)
        @JsonAlias("symbol")
        String stockCode,

        @NotBlank
        @Pattern(regexp = "(?i)PRICE|PCT_CHANGE|TURNOVER|SECTOR_HEAT", message = "must be one of PRICE, PCT_CHANGE, TURNOVER, SECTOR_HEAT")
        String ruleType,

        @NotBlank
        @Pattern(regexp = "(?i)GT|GTE|LT|LTE|>|>=|<|<=", message = "must be one of GT, GTE, LT, LTE, >, >=, <, <=")
        String operator,

        @DecimalMin(value = "0.0", inclusive = false)
        @JsonAlias("thresholdValue")
        BigDecimal threshold,

        Boolean enabled,

        Boolean oncePerDay
) {
}
