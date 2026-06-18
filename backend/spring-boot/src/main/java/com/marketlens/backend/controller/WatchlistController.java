package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.dto.WatchlistStockRequest;
import com.marketlens.backend.model.WatchlistStock;
import com.marketlens.backend.service.WatchlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {
    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ApiResponse<List<WatchlistStock>> list() {
        return ApiResponse.success(watchlistService.list());
    }

    @PostMapping
    public ApiResponse<WatchlistStock> create(@Valid @RequestBody WatchlistStockRequest request) {
        return ApiResponse.success(watchlistService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Positive @PathVariable Long id) {
        watchlistService.delete(id);
        return ApiResponse.success();
    }
}
