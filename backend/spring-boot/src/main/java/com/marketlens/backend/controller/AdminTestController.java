package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.model.MarketReview;
import com.marketlens.backend.model.MockDataSummary;
import com.marketlens.backend.service.AlertEvaluationService;
import com.marketlens.backend.service.MockDataService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/admin")
public class AdminTestController {
    private final AlertEvaluationService alertEvaluationService;
    private final MockDataService mockDataService;

    public AdminTestController(AlertEvaluationService alertEvaluationService, MockDataService mockDataService) {
        this.alertEvaluationService = alertEvaluationService;
        this.mockDataService = mockDataService;
    }

    @PostMapping("/alerts/check")
    public ApiResponse<Map<String, Integer>> checkAlerts() {
        return ApiResponse.success(Map.of("triggered", alertEvaluationService.evaluateEnabledRules()));
    }

    @PostMapping("/market-review/generate")
    public ApiResponse<MarketReview> generateMarketReview() {
        MockDataSummary summary = mockDataService.seedDevelopmentData();
        return ApiResponse.success(summary.review());
    }
}
