package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.dto.AlertRuleRequest;
import com.marketlens.backend.model.AlertRule;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/alert-rules")
public class AlertRuleController {
    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @GetMapping
    public ApiResponse<List<AlertRule>> list() {
        return ApiResponse.success(alertRuleService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AlertRule> get(@Positive @PathVariable Long id) {
        return ApiResponse.success(alertRuleService.get(id));
    }

    @PostMapping
    public ApiResponse<AlertRule> create(@Valid @RequestBody AlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AlertRule> update(@Positive @PathVariable Long id, @Valid @RequestBody AlertRuleRequest request) {
        return ApiResponse.success(alertRuleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Positive @PathVariable Long id) {
        alertRuleService.delete(id);
        return ApiResponse.success();
    }
}
