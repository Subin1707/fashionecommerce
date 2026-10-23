CREATE TABLE IF NOT EXISTS campaign_events (
    id BIGSERIAL PRIMARY KEY,
    event_name VARCHAR(120) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(240),
    desktop_image_url VARCHAR(500) NOT NULL,
    mobile_image_url VARCHAR(500),
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    offer_label VARCHAR(60),
    voucher_code VARCHAR(50),
    target_url VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_campaign_event_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'ENDED', 'HIDDEN')),
    CONSTRAINT chk_campaign_event_dates CHECK (end_at > start_at)
)@@
CREATE INDEX IF NOT EXISTS idx_campaign_event_status_schedule
    ON campaign_events(status, start_at, end_at, priority DESC)@@
