package com.marketlens.backend.marketdata;

public class MarketDataProviderException extends RuntimeException {
    public MarketDataProviderException(String message) {
        super(message);
    }

    public MarketDataProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
