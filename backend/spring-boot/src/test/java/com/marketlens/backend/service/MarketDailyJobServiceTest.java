package com.marketlens.backend.service;

import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import com.marketlens.backend.marketdata.dto.MarketDataIngestResult;
import com.marketlens.backend.model.MarketReview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDailyJobServiceTest {
    @Mock
    private MarketDataIngestService ingestService;
    @Mock
    private MarketReviewService marketReviewService;
    @Mock
    private JobRunLogService jobRunLogService;
    @InjectMocks
    private MarketDailyJobService service;

    @Test
    void createsMarketReviewFromIngestedQuotes() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 19);
        DailyQuoteData quote = new DailyQuoteData(
                "600000", "A", tradeDate,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("3.0"),
                BigDecimal.ONE, BigDecimal.TEN, "AKSHARE"
        );
        when(ingestService.ingest("A", tradeDate)).thenReturn(
                new MarketDataIngestResult("A", tradeDate, "AKSHARE", false, 1, List.of(quote))
        );
        when(marketReviewService.saveOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.run("A", tradeDate);

        ArgumentCaptor<MarketReview> review = ArgumentCaptor.forClass(MarketReview.class);
        verify(marketReviewService).saveOrUpdate(review.capture());
        assertThat(review.getValue().source()).isEqualTo("AKSHARE");
        assertThat(review.getValue().leadingStocks()).containsExactly("600000");
    }
}
