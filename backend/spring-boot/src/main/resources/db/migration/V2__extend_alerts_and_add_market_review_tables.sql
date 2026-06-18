ALTER TABLE alert_rules
    ADD COLUMN IF NOT EXISTS once_per_day BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE alert_events
    ADD COLUMN IF NOT EXISTS trigger_price NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS trigger_pct NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS channel VARCHAR(32) NOT NULL DEFAULT 'LOG';

CREATE TABLE IF NOT EXISTS market_reviews (
    id BIGSERIAL PRIMARY KEY,
    review_date DATE NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    highlights_json TEXT NOT NULL,
    leading_stocks_json TEXT NOT NULL,
    source VARCHAR(64) NOT NULL DEFAULT 'manual',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS research_items (
    id BIGSERIAL PRIMARY KEY,
    review_date DATE NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_name VARCHAR(128),
    url VARCHAR(512),
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_market_reviews_review_date ON market_reviews (review_date DESC);
CREATE INDEX IF NOT EXISTS idx_research_items_review_date ON research_items (review_date DESC);

CREATE TRIGGER trg_market_reviews_updated_at
BEFORE UPDATE ON market_reviews
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_research_items_updated_at
BEFORE UPDATE ON research_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
