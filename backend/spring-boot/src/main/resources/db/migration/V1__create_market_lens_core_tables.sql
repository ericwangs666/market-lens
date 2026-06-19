CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL DEFAULT '',
    email VARCHAR(255) UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stocks (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    market VARCHAR(16) NOT NULL,
    name VARCHAR(128) NOT NULL,
    exchange VARCHAR(32),
    sector VARCHAR(128),
    industry VARCHAR(128),
    currency VARCHAR(16),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stocks_market_symbol UNIQUE (market, symbol)
);

CREATE TABLE watchlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    stock_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL DEFAULT 'default',
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_watchlists_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_watchlists_stock FOREIGN KEY (stock_id) REFERENCES stocks (id),
    CONSTRAINT uk_watchlists_user_stock UNIQUE (user_id, stock_id)
);

CREATE TABLE stock_notes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    stock_id BIGINT NOT NULL,
    title VARCHAR(128),
    content TEXT NOT NULL,
    note_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_notes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_stock_notes_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE TABLE daily_quotes (
    id BIGSERIAL PRIMARY KEY,
    stock_id BIGINT NOT NULL,
    quote_date DATE NOT NULL,
    open_price NUMERIC(18, 4),
    high_price NUMERIC(18, 4),
    low_price NUMERIC(18, 4),
    close_price NUMERIC(18, 4),
    previous_close_price NUMERIC(18, 4),
    change_amount NUMERIC(18, 4),
    pct_change NUMERIC(10, 4),
    volume NUMERIC(24, 4),
    turnover NUMERIC(24, 4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_quotes_stock FOREIGN KEY (stock_id) REFERENCES stocks (id),
    CONSTRAINT uk_daily_quotes_stock_date UNIQUE (stock_id, quote_date)
);

CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 1,
    stock_id BIGINT NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    compare_operator VARCHAR(8) NOT NULL,
    threshold_value NUMERIC(18, 4) NOT NULL,
    message VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_rules_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_alert_rules_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE TABLE alert_events (
    id BIGSERIAL PRIMARY KEY,
    alert_rule_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL DEFAULT 1,
    stock_id BIGINT NOT NULL,
    event_date DATE NOT NULL DEFAULT CURRENT_DATE,
    trigger_value NUMERIC(18, 4),
    message TEXT,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_events_rule FOREIGN KEY (alert_rule_id) REFERENCES alert_rules (id),
    CONSTRAINT fk_alert_events_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_alert_events_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

INSERT INTO users (id, username, display_name, status)
VALUES (1, 'default', 'Default User', 'active')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));

CREATE INDEX idx_stocks_name ON stocks (name);
CREATE INDEX idx_watchlists_user_id ON watchlists (user_id);
CREATE INDEX idx_stock_notes_user_stock ON stock_notes (user_id, stock_id);
CREATE INDEX idx_daily_quotes_quote_date ON daily_quotes (quote_date);
CREATE INDEX idx_alert_rules_user_enabled ON alert_rules (user_id, enabled);
CREATE INDEX idx_alert_events_user_created_at ON alert_events (user_id, created_at);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_stocks_updated_at
BEFORE UPDATE ON stocks
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_watchlists_updated_at
BEFORE UPDATE ON watchlists
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_stock_notes_updated_at
BEFORE UPDATE ON stock_notes
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_daily_quotes_updated_at
BEFORE UPDATE ON daily_quotes
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_alert_rules_updated_at
BEFORE UPDATE ON alert_rules
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_alert_events_updated_at
BEFORE UPDATE ON alert_events
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
