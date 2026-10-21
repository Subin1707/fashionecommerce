CREATE TABLE IF NOT EXISTS return_requests (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    order_item_id BIGINT NOT NULL REFERENCES order_items(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    request_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)@@
CREATE INDEX IF NOT EXISTS idx_return_requests_user ON return_requests(user_id, created_at DESC)@@
CREATE INDEX IF NOT EXISTS idx_return_requests_status ON return_requests(status, created_at DESC)@@
CREATE TABLE IF NOT EXISTS return_request_evidence (
    return_request_id BIGINT NOT NULL REFERENCES return_requests(id) ON DELETE CASCADE,
    file_url VARCHAR(255) NOT NULL,
    PRIMARY KEY (return_request_id, file_url)
)@@
