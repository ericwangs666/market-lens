package com.marketlens.backend.service;

import com.marketlens.backend.dto.WatchlistStockRequest;
import com.marketlens.backend.entity.Stock;
import com.marketlens.backend.entity.Watchlist;
import com.marketlens.backend.mapper.StockMapper;
import com.marketlens.backend.mapper.WatchlistMapper;
import com.marketlens.backend.model.WatchlistStock;
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
class WatchlistServiceTest {
    @Mock
    private WatchlistMapper watchlistMapper;

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private WatchlistService watchlistService;

    @Test
    void createAddsStockToDefaultWatchlist() {
        Watchlist[] inserted = new Watchlist[1];
        Stock stock = new Stock();
        stock.setId(10L);
        stock.setSymbol("NVDA");
        stock.setName("NVIDIA");
        stock.setMarket("US");
        when(stockMapper.selectOne(any())).thenReturn(stock);
        when(stockMapper.selectById(10L)).thenReturn(stock);
        doAnswer(invocation -> {
            Watchlist watchlist = invocation.getArgument(0);
            watchlist.setId(20L);
            inserted[0] = watchlist;
            return 1;
        }).when(watchlistMapper).insert(any(Watchlist.class));
        when(watchlistMapper.selectById(20L)).thenAnswer(invocation -> inserted[0]);

        WatchlistStock result = watchlistService.create(new WatchlistStockRequest("nvda", "NVIDIA", "us"));

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.code()).isEqualTo("NVDA");
        assertThat(result.market()).isEqualTo("US");
    }
}
