package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.entity.AlertEvent;
import com.marketlens.backend.entity.AlertRule;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.AlertEventMapper;
import com.marketlens.backend.mapper.AlertRuleMapper;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.model.MarketQuote;
import com.marketlens.backend.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AlertEvaluationService {
    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationService.class);

    private final AlertRuleMapper alertRuleMapper;
    private final StockMapper stockMapper;
    private final MarketDataService marketDataService;
    private final AlertDedupeService alertDedupeService;
    private final AlertEventMapper alertEventMapper;
    private final NotificationService notificationService;

    public AlertEvaluationService(
            AlertRuleMapper alertRuleMapper,
            StockMapper stockMapper,
            MarketDataService marketDataService,
            AlertDedupeService alertDedupeService,
            AlertEventMapper alertEventMapper,
            NotificationService notificationService
    ) {
        this.alertRuleMapper = alertRuleMapper;
        this.stockMapper = stockMapper;
        this.marketDataService = marketDataService;
        this.alertDedupeService = alertDedupeService;
        this.alertEventMapper = alertEventMapper;
        this.notificationService = notificationService;
    }

    public int evaluateEnabledRules() {
        List<AlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>()
                        .eq(AlertRule::getEnabled, true)
        );

        int triggered = 0;
        for (AlertRule rule : rules) {
            if (evaluateRule(rule)) {
                triggered++;
            }
        }
        return triggered;
    }

    private boolean evaluateRule(AlertRule rule) {
        Stock stock = stockMapper.selectById(rule.getStockId());
        if (stock == null) {
            log.warn("Skipping alert rule {} because stock {} does not exist", rule.getId(), rule.getStockId());
            return false;
        }

        Optional<MarketQuote> quote = marketDataService.findLatestQuote(stock);
        if (quote.isEmpty()) {
            return false;
        }

        BigDecimal currentValue = valueForRule(rule, quote.get());
        if (currentValue == null || !matches(rule.getCompareOperator(), currentValue, rule.getThresholdValue())) {
            return false;
        }

        LocalDate eventDate = quote.get().quoteDate() == null ? LocalDate.now() : quote.get().quoteDate();
        if (Boolean.TRUE.equals(rule.getOncePerDay()) && !alertDedupeService.markIfFirstTrigger(rule.getId(), eventDate)) {
            return false;
        }

        String message = buildMessage(rule, stock, currentValue);
        AlertEvent event = new AlertEvent();
        event.setAlertRuleId(rule.getId());
        event.setUserId(rule.getUserId());
        event.setStockId(rule.getStockId());
        event.setEventDate(eventDate);
        event.setTriggerValue(currentValue);
        event.setTriggerPrice(quote.get().closePrice());
        event.setTriggerPct(quote.get().pctChange());
        event.setChannel("LOG");
        event.setMessage(message);
        alertEventMapper.insert(event);

        notificationService.send(new NotificationMessage("Market Lens alert", message));
        return true;
    }

    private BigDecimal valueForRule(AlertRule rule, MarketQuote quote) {
        return switch (rule.getRuleType()) {
            case "PRICE" -> quote.closePrice();
            case "PCT_CHANGE" -> quote.pctChange();
            case "TURNOVER" -> quote.turnover();
            default -> null;
        };
    }

    private boolean matches(String operator, BigDecimal currentValue, BigDecimal threshold) {
        int compared = currentValue.compareTo(threshold);
        return switch (operator) {
            case "GT" -> compared > 0;
            case "GTE" -> compared >= 0;
            case "LT" -> compared < 0;
            case "LTE" -> compared <= 0;
            default -> false;
        };
    }

    private String buildMessage(AlertRule rule, Stock stock, BigDecimal currentValue) {
        if (StringUtils.hasText(rule.getMessage())) {
            return rule.getMessage();
        }
        return stock.getSymbol()
                + " triggered "
                + rule.getRuleType()
                + " "
                + rule.getCompareOperator()
                + " "
                + rule.getThresholdValue()
                + ", current value "
                + currentValue;
    }
}
