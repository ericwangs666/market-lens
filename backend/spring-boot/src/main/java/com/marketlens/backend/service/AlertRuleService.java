package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.dto.AlertRuleRequest;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.exception.NotFoundException;
import com.marketlens.backend.mapper.AlertRuleMapper;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.model.AlertRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AlertRuleService {
    private static final long DEFAULT_USER_ID = 1L;

    private final AlertRuleMapper alertRuleMapper;
    private final StockMapper stockMapper;

    public AlertRuleService(AlertRuleMapper alertRuleMapper, StockMapper stockMapper) {
        this.alertRuleMapper = alertRuleMapper;
        this.stockMapper = stockMapper;
    }

    public List<AlertRule> list() {
        return alertRuleMapper.selectList(new LambdaQueryWrapper<com.marketlens.backend.entity.AlertRule>()
                        .eq(com.marketlens.backend.entity.AlertRule::getUserId, DEFAULT_USER_ID)
                        .orderByDesc(com.marketlens.backend.entity.AlertRule::getUpdatedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    public AlertRule get(Long id) {
        com.marketlens.backend.entity.AlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || !Objects.equals(rule.getUserId(), DEFAULT_USER_ID)) {
            throw new NotFoundException("Alert rule not found");
        }
        return toModel(rule);
    }

    public AlertRule create(AlertRuleRequest request) {
        Stock stock = findOrCreateStock(request.stockCode().trim().toUpperCase(Locale.ROOT));
        com.marketlens.backend.entity.AlertRule rule = new com.marketlens.backend.entity.AlertRule();
        rule.setUserId(DEFAULT_USER_ID);
        rule.setStockId(stock.getId());
        rule.setRuleType(request.ruleType().trim().toUpperCase(Locale.ROOT));
        rule.setCompareOperator(normalizeOperator(request.operator()));
        rule.setThresholdValue(request.threshold());
        rule.setEnabled(request.enabled() == null || request.enabled());
        rule.setOncePerDay(request.oncePerDay() == null || request.oncePerDay());
        alertRuleMapper.insert(rule);
        return toModel(alertRuleMapper.selectById(rule.getId()));
    }

    public AlertRule update(Long id, AlertRuleRequest request) {
        com.marketlens.backend.entity.AlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || !Objects.equals(rule.getUserId(), DEFAULT_USER_ID)) {
            throw new NotFoundException("Alert rule not found");
        }
        Stock stock = findOrCreateStock(request.stockCode().trim().toUpperCase(Locale.ROOT));
        rule.setStockId(stock.getId());
        rule.setRuleType(request.ruleType().trim().toUpperCase(Locale.ROOT));
        rule.setCompareOperator(normalizeOperator(request.operator()));
        rule.setThresholdValue(request.threshold());
        rule.setEnabled(request.enabled() == null || request.enabled());
        rule.setOncePerDay(request.oncePerDay() == null || request.oncePerDay());
        alertRuleMapper.updateById(rule);
        return toModel(alertRuleMapper.selectById(rule.getId()));
    }

    public void delete(Long id) {
        com.marketlens.backend.entity.AlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || !Objects.equals(rule.getUserId(), DEFAULT_USER_ID)) {
            throw new NotFoundException("Alert rule not found");
        }
        alertRuleMapper.deleteById(id);
    }

    private AlertRule toModel(com.marketlens.backend.entity.AlertRule rule) {
        Stock stock = stockMapper.selectById(rule.getStockId());
        return new AlertRule(
                rule.getId(),
                stock == null ? "" : stock.getSymbol(),
                rule.getRuleType(),
                rule.getCompareOperator(),
                rule.getThresholdValue(),
                rule.getEnabled(),
                rule.getOncePerDay(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    private String normalizeOperator(String operator) {
        return switch (operator.trim().toUpperCase(Locale.ROOT)) {
            case ">" -> "GT";
            case ">=" -> "GTE";
            case "<" -> "LT";
            case "<=" -> "LTE";
            default -> operator.trim().toUpperCase(Locale.ROOT);
        };
    }

    private Stock findOrCreateStock(String symbol) {
        String market = inferMarket(symbol);
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getMarket, market)
                .eq(Stock::getSymbol, symbol)
                .last("LIMIT 1"));
        if (stock != null) {
            return stock;
        }

        Stock created = new Stock();
        created.setSymbol(symbol);
        created.setName(symbol);
        created.setMarket(market);
        created.setStatus("active");
        stockMapper.insert(created);
        return created;
    }

    private String inferMarket(String symbol) {
        return symbol.matches("[A-Z.]+") ? "US" : "CN";
    }
}
