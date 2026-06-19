package com.marketlens.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketlens.backend.controller.MarketDataAdminController;
import com.marketlens.backend.marketdata.AkshareMarketDataProvider;
import com.marketlens.backend.marketdata.MarketDataProperties;
import com.marketlens.backend.marketdata.MarketDataProviderFactory;
import com.marketlens.backend.service.MarketDailyJobService;
import com.marketlens.backend.service.MarketDataIngestService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminTokenInterceptorTest {
    @Test
    void rejectsMissingToken() throws Exception {
        MockMvc mockMvc = mockMvc("test-token");

        mockMvc.perform(get("/api/admin/market-data/providers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void acceptsMatchingToken() throws Exception {
        MarketDataProperties properties = new MarketDataProperties();
        properties.setUseMock(true);
        MockMvc mockMvc = mockMvc("test-token", properties);

        mockMvc.perform(get("/api/admin/market-data/providers")
                        .header("X-Admin-Token", "test-token"))
                .andExpect(status().isOk());
    }

    private MockMvc mockMvc(String token) {
        return mockMvc(token, new MarketDataProperties());
    }

    private MockMvc mockMvc(String token, MarketDataProperties properties) {
        MarketDataProviderFactory providerFactory = mock(MarketDataProviderFactory.class);
        AkshareMarketDataProvider akshareProvider = mock(AkshareMarketDataProvider.class);
        when(providerFactory.getAkshareProvider()).thenReturn(akshareProvider);
        MarketDataAdminController controller = new MarketDataAdminController(
                properties,
                providerFactory,
                mock(MarketDataIngestService.class),
                mock(MarketDailyJobService.class)
        );
        return MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new AdminTokenInterceptor(token, new ObjectMapper()))
                .build();
    }
}
