-- Preserve existing reviews; only new reviews must link to an order item.
DO $$
DECLARE constraint_name text;
BEGIN
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS order_item_id BIGINT REFERENCES order_items(id);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS order_id BIGINT REFERENCES orders(id);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS seller_reply VARCHAR(1000);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS replied_at TIMESTAMP;
  FOR constraint_name IN
    SELECT c.conname FROM pg_constraint c
    WHERE c.conrelid = 'reviews'::regclass AND c.contype = 'u'
      AND (SELECT array_agg(a.attname::text ORDER BY a.attname)
           FROM unnest(c.conkey) k JOIN pg_attribute a
           ON a.attrelid = c.conrelid AND a.attnum = k) = ARRAY['product_id', 'user_id']
  LOOP
    EXECUTE format('ALTER TABLE reviews DROP CONSTRAINT %I', constraint_name);
  END LOOP;
CREATE UNIQUE INDEX IF NOT EXISTS ux_reviews_order_item ON reviews(order_item_id);
CREATE TABLE IF NOT EXISTS review_media (
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    data_url TEXT,
    PRIMARY KEY (review_id, position)
);
END $$@@
