package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.exception.NotFoundException;
import com.marketlens.backend.model.StockSearchResult;
import com.marketlens.backend.service.StockSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/stocks")
public class StockSearchController {
    private final StockSearchService stockSearchService;

    public StockSearchController(StockSearchService stockSearchService) {
        this.stockSearchService = stockSearchService;
    }

    @GetMapping("/search")
    public ApiResponse<List<StockSearchResult>> search(@NotBlank @Size(max = 80) @RequestParam String keyword) {
        return ApiResponse.success(stockSearchService.search(keyword));
    }

    @GetMapping("/{symbol}")
    public ApiResponse<StockSearchResult> get(@NotBlank @Size(max = 20) @org.springframework.web.bind.annotation.PathVariable String symbol) {
        return ApiResponse.success(stockSearchService.findByCode(symbol)
                .orElseThrow(() -> new NotFoundException("Stock not found")));
    }
}
