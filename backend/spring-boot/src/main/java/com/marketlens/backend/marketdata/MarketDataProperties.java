package com.marketlens.backend.marketdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market-data")
public class MarketDataProperties {
    private boolean useMock;
    private boolean fallbackToMock = true;
    private String providerA = "AKSHARE";
    private Akshare akshare = new Akshare();

    public boolean isUseMock() {
        return useMock;
    }

    public void setUseMock(boolean useMock) {
        this.useMock = useMock;
    }

    public boolean isFallbackToMock() {
        return fallbackToMock;
    }

    public void setFallbackToMock(boolean fallbackToMock) {
        this.fallbackToMock = fallbackToMock;
    }

    public String getProviderA() {
        return providerA;
    }

    public void setProviderA(String providerA) {
        this.providerA = providerA;
    }

    public Akshare getAkshare() {
        return akshare;
    }

    public void setAkshare(Akshare akshare) {
        this.akshare = akshare;
    }

    public static class Akshare {
        private String pythonCommand = "python";
        private String workerPath = "../data-worker/fetch_a_daily_quotes.py";

        public String getPythonCommand() {
            return pythonCommand;
        }

        public void setPythonCommand(String pythonCommand) {
            this.pythonCommand = pythonCommand;
        }

        public String getWorkerPath() {
            return workerPath;
        }

        public void setWorkerPath(String workerPath) {
            this.workerPath = workerPath;
        }
    }
}
