package com.marketlens.backend.service;

import com.marketlens.backend.model.StockSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class StockSearchService {
    private static final List<StockSearchResult> MOCK_STOCKS = List.of(
            new StockSearchResult("300244", "迪安诊断", "CN", "stock"),
            new StockSearchResult("601958", "金钼股份", "CN", "stock"),
            new StockSearchResult("002203", "海亮股份", "CN", "stock"),
            new StockSearchResult("000768", "中航西飞", "CN", "stock"),
            new StockSearchResult("601696", "中银证券", "CN", "stock"),
            new StockSearchResult("603990", "麦迪科技", "CN", "stock"),
            new StockSearchResult("NVDA", "NVIDIA", "US", "stock"),
            new StockSearchResult("AVGO", "Broadcom", "US", "stock"),
            new StockSearchResult("MSFT", "Microsoft", "US", "stock"),
            new StockSearchResult("QQQ", "Invesco QQQ Trust", "US", "ETF")
    );

    public List<StockSearchResult> search(String keyword) {
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return MOCK_STOCKS.stream()
                .filter(stock -> stock.code().toLowerCase(Locale.ROOT).contains(normalized)
                        || stock.name().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(20)
                .toList();
    }

    public Optional<StockSearchResult> findByCode(String symbol) {
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return MOCK_STOCKS.stream()
                .filter(stock -> stock.code().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
