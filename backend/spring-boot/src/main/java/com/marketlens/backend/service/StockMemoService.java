package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marketlens.backend.dto.StockMemoRequest;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.entity.StockNote;
import com.marketlens.backend.exception.NotFoundException;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.mapper.StockNoteMapper;
import com.marketlens.backend.model.StockMemo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class StockMemoService {
    private static final long DEFAULT_USER_ID = 1L;

    private final StockNoteMapper stockNoteMapper;
    private final StockMapper stockMapper;

    public StockMemoService(StockNoteMapper stockNoteMapper, StockMapper stockMapper) {
        this.stockNoteMapper = stockNoteMapper;
        this.stockMapper = stockMapper;
    }

    public List<StockMemo> list() {
        return stockNoteMapper.selectList(new LambdaQueryWrapper<StockNote>()
                        .eq(StockNote::getUserId, DEFAULT_USER_ID)
                        .orderByDesc(StockNote::getUpdatedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    public StockMemo get(Long id) {
        StockNote note = stockNoteMapper.selectById(id);
        if (note == null || !Objects.equals(note.getUserId(), DEFAULT_USER_ID)) {
            throw new NotFoundException("Stock memo not found");
        }
        return toModel(note);
    }

    public List<StockMemo> listByStockCode(String stockCode) {
        Stock stock = findStock(stockCode.trim().toUpperCase(Locale.ROOT));
        if (stock == null) {
            return List.of();
        }
        return stockNoteMapper.selectList(new LambdaQueryWrapper<StockNote>()
                        .eq(StockNote::getUserId, DEFAULT_USER_ID)
                        .eq(StockNote::getStockId, stock.getId())
                        .orderByDesc(StockNote::getUpdatedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    public StockMemo create(StockMemoRequest request) {
        Stock stock = findOrCreateStock(request.stockCode().trim().toUpperCase(Locale.ROOT));
        StockNote note = new StockNote();
        note.setUserId(DEFAULT_USER_ID);
        note.setStockId(stock.getId());
        note.setTitle(resolveTitle(request));
        note.setContent(request.content().trim());
        stockNoteMapper.insert(note);
        return toModel(stockNoteMapper.selectById(note.getId()));
    }

    public StockMemo update(Long id, StockMemoRequest request) {
        StockNote note = stockNoteMapper.selectById(id);
        if (note == null || !Objects.equals(note.getUserId(), DEFAULT_USER_ID)) {
            throw new NotFoundException("Stock memo not found");
        }
        Stock stock = findOrCreateStock(request.stockCode().trim().toUpperCase(Locale.ROOT));
        note.setStockId(stock.getId());
        note.setTitle(resolveTitle(request));
        note.setContent(request.content().trim());
        stockNoteMapper.updateById(note);
        return toModel(stockNoteMapper.selectById(note.getId()));
    }

    public void delete(Long id) {
        StockNote note = stockNoteMapper.selectById(id);
        if (note == null || !Objects.equals(note.getUserId(), DEFAULT_USER_ID)) {
            throw new NotFoundException("Stock memo not found");
        }
        stockNoteMapper.deleteById(id);
    }

    private StockMemo toModel(StockNote note) {
        Stock stock = stockMapper.selectById(note.getStockId());
        return new StockMemo(
                note.getId(),
                stock == null ? "" : stock.getSymbol(),
                note.getTitle(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private String resolveTitle(StockMemoRequest request) {
        if (StringUtils.hasText(request.title())) {
            return request.title().trim();
        }
        return request.stockCode().trim().toUpperCase(Locale.ROOT);
    }

    private Stock findStock(String symbol) {
        return stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getMarket, inferMarket(symbol))
                .eq(Stock::getSymbol, symbol)
                .last("LIMIT 1"));
    }

    private Stock findOrCreateStock(String symbol) {
        Stock stock = findStock(symbol);
        if (stock != null) {
            return stock;
        }

        Stock created = new Stock();
        created.setSymbol(symbol);
        created.setName(symbol);
        created.setMarket(inferMarket(symbol));
        created.setStatus("active");
        stockMapper.insert(created);
        return created;
    }

    private String inferMarket(String symbol) {
        return symbol.matches("[A-Z.]+") ? "US" : "CN";
    }
}
