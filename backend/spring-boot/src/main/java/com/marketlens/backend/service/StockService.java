package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.StockMapper;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class StockService {
    private final StockMapper stockMapper;

    public StockService(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    public Stock findOrCreate(String symbol, String name, String market) {
        String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        String normalizedMarket = market.trim().toUpperCase(Locale.ROOT);
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getMarket, normalizedMarket)
                .eq(Stock::getSymbol, normalizedSymbol)
                .last("LIMIT 1"));
        if (stock != null) {
            return stock;
        }

        Stock created = new Stock();
        created.setSymbol(normalizedSymbol);
        created.setName(name.trim());
        created.setMarket(normalizedMarket);
        created.setStatus("active");
        stockMapper.insert(created);
        return created;
    }
}
