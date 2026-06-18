package com.marketlens.backend.controller;

import com.marketlens.backend.common.ApiResponse;
import com.marketlens.backend.dto.StockMemoRequest;
import com.marketlens.backend.model.StockMemo;
import com.marketlens.backend.service.StockMemoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/notes")
public class NotesController {
    private final StockMemoService stockMemoService;

    public NotesController(StockMemoService stockMemoService) {
        this.stockMemoService = stockMemoService;
    }

    @GetMapping
    public ApiResponse<List<StockMemo>> list() {
        return ApiResponse.success(stockMemoService.list());
    }

    @GetMapping("/{symbol}")
    public ApiResponse<List<StockMemo>> listBySymbol(@NotBlank @Size(max = 20) @PathVariable String symbol) {
        return ApiResponse.success(stockMemoService.listByStockCode(symbol));
    }

    @PostMapping
    public ApiResponse<StockMemo> create(@Valid @RequestBody StockMemoRequest request) {
        return ApiResponse.success(stockMemoService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StockMemo> update(@Positive @PathVariable Long id, @Valid @RequestBody StockMemoRequest request) {
        return ApiResponse.success(stockMemoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Positive @PathVariable Long id) {
        stockMemoService.delete(id);
        return ApiResponse.success();
    }
}
