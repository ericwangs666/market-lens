package com.marketlens.backend.marketdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketlens.backend.marketdata.dto.DailyQuoteData;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class AkshareMarketDataProvider implements MarketDataProvider {
    private static final TypeReference<List<DailyQuoteData>> QUOTE_LIST = new TypeReference<>() {
    };

    private final MarketDataProperties properties;
    private final ObjectMapper objectMapper;

    public AkshareMarketDataProvider(MarketDataProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "AKSHARE";
    }

    @Override
    public List<DailyQuoteData> fetchDailyQuotes(String market, LocalDate tradeDate, List<String> symbols) {
        if (!"A".equalsIgnoreCase(market)) {
            throw new MarketDataProviderException("AKShare provider only supports market A");
        }
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }

        Path workerPath = resolveWorkerPath();
        List<String> command = new ArrayList<>();
        command.add(properties.getAkshare().getPythonCommand());
        command.add(workerPath.toString());
        command.add("--market");
        command.add("A");
        command.add("--tradeDate");
        command.add(tradeDate.toString());
        command.add("--symbols");
        command.add(String.join(",", symbols));

        ProcessResult result = runProcess(command, workerPath.getParent());
        if (result.exitCode() != 0) {
            String details = StringUtils.hasText(result.stderr()) ? result.stderr() : result.stdout();
            throw new MarketDataProviderException("AKShare worker failed: " + details.trim());
        }

        try {
            return objectMapper.readValue(result.stdout(), QUOTE_LIST);
        } catch (IOException exception) {
            throw new MarketDataProviderException("AKShare worker returned invalid JSON", exception);
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getAkshare().getPythonCommand())
                && Files.isRegularFile(resolveWorkerPath());
    }

    protected ProcessResult runProcess(List<String> command, Path workingDirectory) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .start();
            CompletableFuture<String> stdout = readAsync(process.getInputStream());
            CompletableFuture<String> stderr = readAsync(process.getErrorStream());
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, stdout.join(), stderr.join());
        } catch (IOException exception) {
            throw new MarketDataProviderException("Unable to start AKShare worker", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MarketDataProviderException("AKShare worker was interrupted", exception);
        }
    }

    private CompletableFuture<String> readAsync(java.io.InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (inputStream) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new MarketDataProviderException("Unable to read AKShare worker output", exception);
            }
        });
    }

    private Path resolveWorkerPath() {
        Path configured = Path.of(properties.getAkshare().getWorkerPath());
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        Path current = Path.of("").toAbsolutePath().normalize();
        Path direct = current.resolve(configured).normalize();
        if (Files.isRegularFile(direct)) {
            return direct;
        }

        String fileName = configured.getFileName().toString();
        for (Path base = current; base != null; base = base.getParent()) {
            Path candidate = base.resolve("data-worker").resolve(fileName).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return direct;
    }

    protected record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
