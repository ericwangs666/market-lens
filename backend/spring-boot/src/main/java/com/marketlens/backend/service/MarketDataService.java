package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.entity.DailyQuote;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.DailyQuoteMapper;
import com.marketlens.backend.model.MarketQuote;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MarketDataService {
    private final DailyQuoteMapper dailyQuoteMapper;

    public MarketDataService(DailyQuoteMapper dailyQuoteMapper) {
        this.dailyQuoteMapper = dailyQuoteMapper;
    }

    public Optional<MarketQuote> findLatestQuote(Stock stock) {
        DailyQuote quote = dailyQuoteMapper.selectOne(
                new LambdaQueryWrapper<DailyQuote>()
                        .eq(DailyQuote::getStockId, stock.getId())
                        .orderByDesc(DailyQuote::getQuoteDate)
                        .last("LIMIT 1")
        );

        if (quote == null) {
            return Optional.empty();
        }

        return Optional.of(new MarketQuote(
                stock.getId(),
                stock.getSymbol(),
                quote.getQuoteDate(),
                quote.getClosePrice(),
                quote.getPctChange(),
                quote.getTurnover()
        ));
    }
}
