package com.marketlens.backend.marketdata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MarketDataProviderFactoryTest {
    @Test
    void selectsMockWhenUseMockIsEnabled() {
        MarketDataProperties properties = new MarketDataProperties();
        properties.setUseMock(true);
        AkshareMarketDataProvider akshare = mock(AkshareMarketDataProvider.class);
        MockMarketDataProvider mock = new MockMarketDataProvider();

        MarketDataProviderFactory factory = new MarketDataProviderFactory(properties, akshare, mock);

        assertThat(factory.getProvider("A")).isSameAs(mock);
    }

    @Test
    void selectsAkshareForAShares() {
        MarketDataProperties properties = new MarketDataProperties();
        properties.setUseMock(false);
        properties.setProviderA("AKSHARE");
        AkshareMarketDataProvider akshare = mock(AkshareMarketDataProvider.class);
        MockMarketDataProvider mock = new MockMarketDataProvider();

        MarketDataProviderFactory factory = new MarketDataProviderFactory(properties, akshare, mock);

        assertThat(factory.getProvider("A")).isSameAs(akshare);
    }
}
