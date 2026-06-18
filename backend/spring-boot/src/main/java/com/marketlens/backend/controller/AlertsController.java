package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.dto.AlertRuleRequest;
import com.marketlens.backend.model.AlertHistory;
import com.marketlens.backend.model.AlertRule;
import com.marketlens.backend.service.AlertHistoryService;
import com.marketlens.backend.service.AlertRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/alerts")
public class AlertsController {
    private final AlertRuleService alertRuleService;
    private final AlertHistoryService alertHistoryService;

    public AlertsController(AlertRuleService alertRuleService, AlertHistoryService alertHistoryService) {
        this.alertRuleService = alertRuleService;
        this.alertHistoryService = alertHistoryService;
    }

    @GetMapping("/rules")
    public ApiResponse<List<AlertRule>> listRules() {
        return ApiResponse.success(alertRuleService.list());
    }

    @GetMapping("/rules/{id}")
    public ApiResponse<AlertRule> getRule(@Positive @PathVariable Long id) {
        return ApiResponse.success(alertRuleService.get(id));
    }

    @PostMapping("/rules")
    public ApiResponse<AlertRule> createRule(@Valid @RequestBody AlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.create(request));
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<AlertRule> updateRule(@Positive @PathVariable Long id, @Valid @RequestBody AlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.update(id, request));
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@Positive @PathVariable Long id) {
        alertRuleService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/events")
    public ApiResponse<List<AlertHistory>> listEvents(@RequestParam(required = false) String symbol) {
        return ApiResponse.success(alertHistoryService.list(symbol));
    }
}
