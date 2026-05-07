CREATE TABLE IF NOT EXISTS bets (
    id           BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      VARCHAR(64)     NOT NULL,
    event_id     VARCHAR(64)     NOT NULL,
    market_id    VARCHAR(64)     NOT NULL,
    winner_id    VARCHAR(64)     NOT NULL,
    amount       DECIMAL(15, 2)  NOT NULL,
    status       VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    payout       DECIMAL(15, 2)  NULL,
    settled_at   TIMESTAMP       NULL,
    created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bets_event_status ON bets (event_id, status);
