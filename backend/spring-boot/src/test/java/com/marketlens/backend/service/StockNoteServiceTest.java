package com.marketlens.backend.service;

import com.marketlens.backend.dto.StockMemoRequest;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.entity.StockNote;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.mapper.StockNoteMapper;
import com.marketlens.backend.model.StockMemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockNoteServiceTest {
    @Mock
    private StockNoteMapper stockNoteMapper;

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private StockMemoService stockMemoService;

    @Test
    void createStoresMemoForStock() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setSymbol("NVDA");
        when(stockMapper.selectOne(any())).thenReturn(stock);
        when(stockMapper.selectById(1L)).thenReturn(stock);
        doAnswer(invocation -> {
            StockNote note = invocation.getArgument(0);
            note.setId(2L);
            return 1;
        }).when(stockNoteMapper).insert(any(StockNote.class));
        when(stockNoteMapper.selectById(2L)).thenAnswer(invocation -> {
            StockNote note = new StockNote();
            note.setId(2L);
            note.setUserId(1L);
            note.setStockId(1L);
            note.setTitle("Thesis");
            note.setContent("Watch earnings");
            return note;
        });

        StockMemo result = stockMemoService.create(new StockMemoRequest("NVDA", "Thesis", "Watch earnings"));

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.stockCode()).isEqualTo("NVDA");
        assertThat(result.content()).isEqualTo("Watch earnings");
    }
}
