package com.marketlens.backend.marketdata.dto;

public record MarketDataProviderStatus(
        String providerA,
        boolean useMock,
        boolean fallbackToMock,
        boolean akshareWorkerConfigured
) {
}
