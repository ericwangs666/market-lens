package com.marketlens.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("watchlists")
public class Watchlist extends BaseEntity {

    private Long userId;

    private Long stockId;

    private String name;

    private Integer sortOrder;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
