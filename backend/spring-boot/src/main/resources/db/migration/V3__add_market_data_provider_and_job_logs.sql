ALTER TABLE daily_quotes
    ADD COLUMN IF NOT EXISTS market VARCHAR(16),
    ADD COLUMN IF NOT EXISTS symbol VARCHAR(32),
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN';

UPDATE daily_quotes q
SET market = s.market,
    symbol = s.symbol
FROM stocks s
WHERE q.stock_id = s.id
  AND (q.market IS NULL OR q.symbol IS NULL);

ALTER TABLE daily_quotes
    ALTER COLUMN market SET NOT NULL,
    ALTER COLUMN symbol SET NOT NULL;

ALTER TABLE daily_quotes
    ADD CONSTRAINT uk_daily_quotes_market_symbol_date
        UNIQUE (market, symbol, quote_date);

CREATE TABLE job_run_logs (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(64) NOT NULL,
    market VARCHAR(16),
    trade_date DATE,
    status VARCHAR(16) NOT NULL,
    provider VARCHAR(32),
    records_count INTEGER NOT NULL DEFAULT 0,
    message TEXT,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_run_logs_job_trade_date
    ON job_run_logs (job_name, trade_date DESC);

CREATE TRIGGER trg_job_run_logs_updated_at
BEFORE UPDATE ON job_run_logs
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
