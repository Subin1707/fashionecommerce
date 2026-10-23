-- Preserve existing reviews; only new reviews must link to an order item.
DO $$
DECLARE constraint_name text;
BEGIN
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS order_item_id BIGINT REFERENCES order_items(id);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS order_id BIGINT REFERENCES orders(id);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS seller_reply VARCHAR(1000);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS replied_at TIMESTAMP;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS rating SMALLINT NOT NULL DEFAULT 5;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS comment TEXT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
-- Old installations may contain more than one review for an order item. Keep
-- every row and allow the unique index to be added only after the data is safe.
IF EXISTS (
    SELECT 1 FROM reviews
    WHERE order_item_id IS NOT NULL
    GROUP BY order_item_id
    HAVING COUNT(*) > 1
) THEN
    RAISE EXCEPTION 'Cannot enforce one review per order item: duplicate order_item_id values exist in reviews';
END IF;
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
CREATE TABLE IF NOT EXISTS review_images (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
END $$@@

-- Preserve historical image records while moving them to the collection table.
INSERT INTO review_media(review_id, position, data_url)
SELECT ri.review_id,
       ROW_NUMBER() OVER (PARTITION BY ri.review_id ORDER BY ri.id) - 1,
       ri.image_url
FROM review_images ri
WHERE NOT EXISTS (
    SELECT 1 FROM review_media rm
    WHERE rm.review_id = ri.review_id
      AND rm.data_url = ri.image_url
);
