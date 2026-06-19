package com.marketlens.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("alert_events")
public class AlertEvent extends BaseEntity {

    private Long alertRuleId;

    private Long userId;

    private Long stockId;

    private LocalDate eventDate;

    private BigDecimal triggerValue;

    private BigDecimal triggerPrice;

    private BigDecimal triggerPct;

    private String message;

    private String channel;

    private LocalDateTime readAt;

    public Long getAlertRuleId() {
        return alertRuleId;
    }

    public void setAlertRuleId(Long alertRuleId) {
        this.alertRuleId = alertRuleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public BigDecimal getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(BigDecimal triggerValue) {
        this.triggerValue = triggerValue;
    }

    public BigDecimal getTriggerPrice() {
        return triggerPrice;
    }

    public void setTriggerPrice(BigDecimal triggerPrice) {
        this.triggerPrice = triggerPrice;
    }

    public BigDecimal getTriggerPct() {
        return triggerPct;
    }

    public void setTriggerPct(BigDecimal triggerPct) {
        this.triggerPct = triggerPct;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
