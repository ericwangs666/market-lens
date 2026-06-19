package com.marketlens.backend.marketdata;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MarketDataProviderFactory {
    private final MarketDataProperties properties;
    private final AkshareMarketDataProvider akshareProvider;
    private final MockMarketDataProvider mockProvider;

    public MarketDataProviderFactory(
            MarketDataProperties properties,
            AkshareMarketDataProvider akshareProvider,
            MockMarketDataProvider mockProvider
    ) {
        this.properties = properties;
        this.akshareProvider = akshareProvider;
        this.mockProvider = mockProvider;
    }

    public MarketDataProvider getProvider(String market) {
        if (properties.isUseMock()) {
            return mockProvider;
        }
        if ("A".equalsIgnoreCase(market)
                && "AKSHARE".equals(properties.getProviderA().toUpperCase(Locale.ROOT))) {
            return akshareProvider;
        }
        return mockProvider;
    }

    public AkshareMarketDataProvider getAkshareProvider() {
        return akshareProvider;
    }

    public MockMarketDataProvider getMockProvider() {
        return mockProvider;
    }
}
