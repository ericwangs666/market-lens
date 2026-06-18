package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.model.AlertHistory;
import com.marketlens.backend.service.AlertHistoryService;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/alert-history")
public class AlertHistoryController {
    private final AlertHistoryService alertHistoryService;

    public AlertHistoryController(AlertHistoryService alertHistoryService) {
        this.alertHistoryService = alertHistoryService;
    }

    @GetMapping
    public ApiResponse<List<AlertHistory>> list(@Size(max = 20) @RequestParam(required = false) String stockCode) {
        return ApiResponse.success(alertHistoryService.list(stockCode));
    }
}
