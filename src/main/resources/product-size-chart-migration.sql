CREATE TABLE IF NOT EXISTS product_size_charts (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    size_label VARCHAR(20) NOT NULL,
    min_chest DOUBLE PRECISION NOT NULL,
    max_chest DOUBLE PRECISION NOT NULL,
    min_waist DOUBLE PRECISION NOT NULL,
    max_waist DOUBLE PRECISION NOT NULL,
    min_hip DOUBLE PRECISION NOT NULL,
    max_hip DOUBLE PRECISION NOT NULL,
    CONSTRAINT uk_product_size_chart UNIQUE (product_id, size_label)
)@@
