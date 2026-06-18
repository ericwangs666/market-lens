package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.dto.WatchlistStockRequest;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.entity.Watchlist;
import com.marketlens.backend.exception.NotFoundException;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.mapper.WatchlistMapper;
import com.marketlens.backend.model.WatchlistStock;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class WatchlistService {
    private static final long DEFAULT_USER_ID = 1L;

    private final WatchlistMapper watchlistMapper;
    private final StockMapper stockMapper;

    public WatchlistService(WatchlistMapper watchlistMapper, StockMapper stockMapper) {
        this.watchlistMapper = watchlistMapper;
        this.stockMapper = stockMapper;
    }

    public List<WatchlistStock> list() {
        return watchlistMapper.selectList(new LambdaQueryWrapper<Watchlist>()
                        .eq(Watchlist::getUserId, DEFAULT_USER_ID)
                        .orderByDesc(Watchlist::getCreatedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    public WatchlistStock create(WatchlistStockRequest request) {
        String normalizedCode = request.code().trim().toUpperCase(Locale.ROOT);
        Stock stock = findOrCreateStock(
                normalizedCode,
                request.name() == null || request.name().isBlank() ? normalizedCode : request.name().trim(),
                request.market().trim().toUpperCase(Locale.ROOT)
        );
        Watchlist watchlist = new Watchlist();
        watchlist.setUserId(DEFAULT_USER_ID);
        watchlist.setStockId(stock.getId());
        watchlist.setName("default");
        watchlistMapper.insert(watchlist);
        return toModel(watchlistMapper.selectById(watchlist.getId()));
    }

    public void delete(Long id) {
        Watchlist watchlist = watchlistMapper.selectById(id);
        if (watchlist == null || !Objects.equals(watchlist.getUserId(), DEFAULT_USER_ID)) {
            throw new NotFoundException("Watchlist stock not found");
        }
        watchlistMapper.deleteById(id);
    }

    private WatchlistStock toModel(Watchlist watchlist) {
        Stock stock = stockMapper.selectById(watchlist.getStockId());
        return new WatchlistStock(
                watchlist.getId(),
                stock == null ? "" : stock.getSymbol(),
                stock == null ? "" : stock.getName(),
                stock == null ? "" : stock.getMarket(),
                watchlist.getCreatedAt()
        );
    }

    private Stock findOrCreateStock(String symbol, String name, String market) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getMarket, market)
                .eq(Stock::getSymbol, symbol)
                .last("LIMIT 1"));
        if (stock != null) {
            return stock;
        }

        Stock created = new Stock();
        created.setSymbol(symbol);
        created.setName(name);
        created.setMarket(market);
        created.setStatus("active");
        stockMapper.insert(created);
        return created;
    }
}
