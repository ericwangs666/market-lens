package com.marketlens.backend.service;

import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.mapper.StockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {
    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private StockService stockService;

    @Test
    void findOrCreateReturnsExistingStock() {
        Stock existing = new Stock();
        existing.setId(1L);
        existing.setSymbol("NVDA");
        existing.setName("NVIDIA");
        existing.setMarket("US");
        when(stockMapper.selectOne(any())).thenReturn(existing);

        Stock result = stockService.findOrCreate("nvda", "NVIDIA", "us");

        assertThat(result).isSameAs(existing);
    }

    @Test
    void findOrCreateInsertsNormalizedStockWhenMissing() {
        when(stockMapper.selectOne(any())).thenReturn(null);

        Stock result = stockService.findOrCreate(" mock1 ", "Mock Alert Trigger", " us ");

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockMapper).insert(captor.capture());
        assertThat(captor.getValue().getSymbol()).isEqualTo("MOCK1");
        assertThat(captor.getValue().getMarket()).isEqualTo("US");
        assertThat(result.getStatus()).isEqualTo("active");
    }
}
