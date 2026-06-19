package com.marketlens.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

@TableName("market_reviews")
public class MarketReviewEntity extends BaseEntity {
    private LocalDate reviewDate;
    private String title;
    private String highlightsJson;
    private String leadingStocksJson;
    private String source;

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHighlightsJson() {
        return highlightsJson;
    }

    public void setHighlightsJson(String highlightsJson) {
        this.highlightsJson = highlightsJson;
    }

    public String getLeadingStocksJson() {
        return leadingStocksJson;
    }

    public void setLeadingStocksJson(String leadingStocksJson) {
        this.leadingStocksJson = leadingStocksJson;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
