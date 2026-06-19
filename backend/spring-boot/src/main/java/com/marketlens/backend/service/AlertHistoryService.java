package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.entity.AlertEvent;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.AlertEventMapper;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.model.AlertHistory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class AlertHistoryService {
    private static final long DEFAULT_USER_ID = 1L;

    private final AlertEventMapper alertEventMapper;
    private final StockMapper stockMapper;

    public AlertHistoryService(AlertEventMapper alertEventMapper, StockMapper stockMapper) {
        this.alertEventMapper = alertEventMapper;
        this.stockMapper = stockMapper;
    }

    public List<AlertHistory> list(String stockCode) {
        LambdaQueryWrapper<AlertEvent> query = new LambdaQueryWrapper<AlertEvent>()
                .eq(AlertEvent::getUserId, DEFAULT_USER_ID)
                .orderByDesc(AlertEvent::getCreatedAt);
        if (StringUtils.hasText(stockCode)) {
            Stock stock = findStock(stockCode.trim().toUpperCase(Locale.ROOT));
            if (stock == null) {
                return List.of();
            }
            query.eq(AlertEvent::getStockId, stock.getId());
        }
        return alertEventMapper.selectList(query).stream()
                .map(this::toModel)
                .toList();
    }

    private AlertHistory toModel(AlertEvent event) {
        Stock stock = stockMapper.selectById(event.getStockId());
        return new AlertHistory(
                event.getId(),
                event.getAlertRuleId(),
                stock == null ? "" : stock.getSymbol(),
                event.getTriggerPrice(),
                event.getTriggerPct(),
                event.getTriggerValue(),
                event.getChannel(),
                event.getMessage(),
                event.getCreatedAt()
        );
    }

    private Stock findStock(String symbol) {
        String market = symbol.matches("[A-Z.]+") ? "US" : "CN";
        return stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getMarket, market)
                .eq(Stock::getSymbol, symbol)
                .last("LIMIT 1"));
    }
}
