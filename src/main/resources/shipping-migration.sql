CREATE TABLE IF NOT EXISTS order_status_history (
 id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES orders(id),
 status VARCHAR(20) NOT NULL, description VARCHAR(500) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)@@
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_lat DOUBLE PRECISION@@
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_lng DOUBLE PRECISION@@
CREATE TABLE IF NOT EXISTS shipping_providers (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL, code VARCHAR(30) NOT NULL UNIQUE,
 logo_url VARCHAR(500), status VARCHAR(20) NOT NULL
)@@
INSERT INTO shipping_providers(name, code, status) VALUES
 ('Giao Hàng Nhanh', 'GHN', 'ACTIVE'), ('Giao Hàng Tiết Kiệm', 'GHTK', 'ACTIVE'), ('Viettel Post', 'VTP', 'ACTIVE')
 ON CONFLICT (code) DO NOTHING@@
CREATE TABLE IF NOT EXISTS shipments (
 id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id),
 provider_id BIGINT NOT NULL REFERENCES shipping_providers(id), tracking_code VARCHAR(100) NOT NULL UNIQUE,
 status VARCHAR(30) NOT NULL, shipping_fee DOUBLE PRECISION NOT NULL CHECK (shipping_fee >= 0),
 weight_kg DOUBLE PRECISION NOT NULL CHECK (weight_kg > 0), estimated_delivery DATE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS provider_id BIGINT@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS tracking_code VARCHAR(100)@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING'@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS shipping_fee DOUBLE PRECISION NOT NULL DEFAULT 0@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS weight_kg DOUBLE PRECISION NOT NULL DEFAULT 1@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS estimated_delivery DATE@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS pickup_lat DOUBLE PRECISION@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS pickup_lng DOUBLE PRECISION@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS delivery_lat DOUBLE PRECISION@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS delivery_lng DOUBLE PRECISION@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS current_lat DOUBLE PRECISION@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS current_lng DOUBLE PRECISION@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS route_index INTEGER NOT NULL DEFAULT 0@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP@@
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP@@
-- Backfill missing provider references using the provider's stable code, not an assumed ID.
INSERT INTO shipping_providers(name, code, status) VALUES ('Giao Hàng Nhanh', 'GHN', 'ACTIVE')
ON CONFLICT (code) DO NOTHING@@
UPDATE shipments
SET provider_id = COALESCE((SELECT id FROM shipping_providers WHERE code = 'GHN'),
                           (SELECT MIN(id) FROM shipping_providers))
WHERE provider_id IS NULL@@
ALTER TABLE shipments ALTER COLUMN provider_id SET NOT NULL@@
CREATE TABLE IF NOT EXISTS shipment_tracking (
 id BIGSERIAL PRIMARY KEY, shipment_id BIGINT NOT NULL REFERENCES shipments(id),
 status VARCHAR(30) NOT NULL, description VARCHAR(500) NOT NULL, location VARCHAR(200),
 latitude DOUBLE PRECISION, longitude DOUBLE PRECISION,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)@@
ALTER TABLE shipment_tracking ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING'@@
ALTER TABLE shipment_tracking ADD COLUMN IF NOT EXISTS description VARCHAR(500) NOT NULL DEFAULT ''@@
ALTER TABLE shipment_tracking ADD COLUMN IF NOT EXISTS location VARCHAR(200)@@
ALTER TABLE shipment_tracking ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP@@
ALTER TABLE shipment_tracking ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION@@
ALTER TABLE shipment_tracking ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION@@
ALTER TABLE shipment_tracking ALTER COLUMN shipment_id SET NOT NULL@@
ALTER TABLE shipment_tracking ALTER COLUMN status SET NOT NULL@@
ALTER TABLE shipment_tracking ALTER COLUMN description SET NOT NULL@@
CREATE INDEX IF NOT EXISTS idx_order_history ON order_status_history(order_id, created_at, id)@@
CREATE INDEX IF NOT EXISTS idx_shipment_tracking ON shipment_tracking(shipment_id, created_at, id)@@

CREATE TABLE IF NOT EXISTS shipment_route_points (
 id BIGSERIAL PRIMARY KEY,
 shipment_id BIGINT NOT NULL REFERENCES shipments(id),
 point_index INTEGER NOT NULL,
 latitude DOUBLE PRECISION NOT NULL,
 longitude DOUBLE PRECISION NOT NULL,
 UNIQUE(shipment_id, point_index)
)@@
CREATE INDEX IF NOT EXISTS idx_shipment_route ON shipment_route_points(shipment_id, point_index)@@
