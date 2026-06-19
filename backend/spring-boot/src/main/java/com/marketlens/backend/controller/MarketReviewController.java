package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.model.MarketReview;
import com.marketlens.backend.service.MarketReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketReviewController {
    private final MarketReviewService marketReviewService;

    public MarketReviewController(MarketReviewService marketReviewService) {
        this.marketReviewService = marketReviewService;
    }

    @GetMapping("/daily-review")
    public ApiResponse<MarketReview> getDailyReview() {
        return ApiResponse.success(marketReviewService.getLatest());
    }
}
