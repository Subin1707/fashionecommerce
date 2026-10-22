-- Vietnamese traditional clothing demo catalog. Run after database/seed.sql and migrations.
-- Wikimedia Commons images use 500px thumbnails and include attribution/license in alt_text.
BEGIN;

INSERT INTO brands (name, slug, description, is_active)
VALUES ('Di Sản Việt Demo', 'di-san-viet-demo', 'Thương hiệu demo cho catalog trang phục truyền thống Việt Nam.', TRUE)
ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, description=EXCLUDED.description, is_active=TRUE;

INSERT INTO categories (name, slug, description, is_active) VALUES
('Việt phục cổ', 'viet-phuc-co-demo', 'Mẫu demo lấy cảm hứng từ kiểu áo truyền thống; không khẳng định phục dựng lịch sử.', TRUE),
('Áo dài truyền thống', 'ao-dai-truyen-thong-demo', 'Áo dài và biến thể lấy cảm hứng từ trang phục Việt.', TRUE),
('Áo bà ba', 'ao-ba-ba-demo', 'Trang phục áo bà ba lấy cảm hứng từ đời sống Nam Bộ.', TRUE),
('Trang phục lễ hội Việt', 'trang-phuc-le-hoi-viet-demo', 'Trang phục demo dùng trong dịp lễ, cưới và sự kiện văn hóa.', TRUE),
('Phụ kiện Việt truyền thống', 'phu-kien-viet-truyen-thong-demo', 'Phụ kiện phối cùng Việt phục.', TRUE)
ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, description=EXCLUDED.description, is_active=TRUE;

INSERT INTO collections (name, slug, description, season, is_featured)
VALUES ('Nét Việt truyền thống', 'net-viet-truyen-thong-demo', 'Catalog demo trang phục lấy cảm hứng từ các kiểu Việt phục.', 'ALL_SEASON', TRUE)
ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, description=EXCLUDED.description, is_featured=TRUE;

CREATE TEMP TABLE vietnamese_traditional_seed (
 slug text PRIMARY KEY, category_slug text NOT NULL, name text NOT NULL, description text NOT NULL,
 base_price numeric(12,2) NOT NULL, sale_price numeric(12,2), material text NOT NULL,
 fit text NOT NULL, gender text NOT NULL, size_group text NOT NULL
) ON COMMIT DROP;

INSERT INTO vietnamese_traditional_seed VALUES
('viet-phuc-ngu-than-nam-demo','viet-phuc-co-demo','Áo ngũ thân nam lấy cảm hứng truyền thống','Mẫu demo tham khảo phom áo ngũ thân; không đại diện cho bản phục dựng lịch sử cụ thể.',1250000,1150000,'Lụa pha','Dáng suông','MALE','apparel'),
('viet-phuc-ngu-than-nu-demo','viet-phuc-co-demo','Áo ngũ thân nữ lấy cảm hứng truyền thống','Mẫu demo áo ngũ thân nữ với phom suông và hàng khuy phía trước.',1280000,1190000,'Lụa pha','Dáng suông','FEMALE','apparel'),
('viet-phuc-ao-tac-nam-demo','viet-phuc-co-demo','Áo tấc nam tham khảo','Trang phục demo lấy cảm hứng áo tấc; thông số chỉ mang tính tham khảo.',1680000,1590000,'Gấm pha','Dáng rộng','MALE','apparel'),
('viet-phuc-ao-tac-nu-demo','viet-phuc-co-demo','Áo tấc nữ tham khảo','Trang phục demo lấy cảm hứng áo tấc; không khẳng định độ chính xác lịch sử.',1750000,1650000,'Lụa pha','Dáng rộng','FEMALE','apparel'),
('viet-phuc-giao-linh-demo','viet-phuc-co-demo','Áo giao lĩnh cách tân','Thiết kế demo gợi cảm hứng giao lĩnh, là sản phẩm tham khảo hiện đại.',1390000,1290000,'Cotton pha','Dáng suông','UNISEX','apparel'),
('viet-phuc-vien-linh-demo','viet-phuc-co-demo','Áo viên lĩnh cách tân','Mẫu demo cổ tròn lấy cảm hứng áo viên lĩnh; không phải hiện vật hay phục dựng.',1420000,1320000,'Gấm pha','Dáng suông','UNISEX','apparel'),
('viet-phuc-doi-kham-demo','viet-phuc-co-demo','Áo đối khâm cách tân','Trang phục demo lấy cảm hứng kiểu áo đối khâm, dùng để phối đồ và chụp ảnh.',1480000,1380000,'Lụa pha','Dáng rộng','FEMALE','apparel'),
('viet-phuc-nhat-binh-demo','viet-phuc-co-demo','Áo lễ hội cảm hứng Nhật Bình','Thiết kế demo lấy cảm hứng áo Nhật Bình; không khẳng định độ chính xác lịch sử.',2150000,1990000,'Gấm pha','Dáng rộng','FEMALE','apparel'),
('ao-dai-nu-co-dien-demo','ao-dai-truyen-thong-demo','Áo dài nữ cổ điển','Áo dài demo phom truyền thống, phối quần dài; số đo cần đối chiếu thực tế.',1180000,1090000,'Lụa pha','Ôm vừa','FEMALE','apparel'),
('ao-dai-nam-co-dung-demo','ao-dai-truyen-thong-demo','Áo dài nam cổ đứng','Áo dài nam demo với cổ đứng và thân dài, phù hợp dịp lễ và chụp ảnh.',1280000,1190000,'Gấm pha','Dáng suông','MALE','apparel'),
('ao-dai-ngu-than-demo','ao-dai-truyen-thong-demo','Áo dài ngũ thân tham khảo','Mẫu demo lấy cảm hứng áo dài ngũ thân; không thay thế tư liệu chuyên khảo.',1580000,1490000,'Lụa pha','Dáng suông','UNISEX','apparel'),
('ao-dai-khan-van-demo','ao-dai-truyen-thong-demo','Áo dài phối khăn vấn','Bộ demo áo dài và khăn vấn phối cùng tông màu; phụ kiện bán theo lựa chọn.',1490000,1390000,'Lụa pha','Ôm vừa','FEMALE','apparel'),
('ao-dai-cuoi-do-demo','trang-phuc-le-hoi-viet-demo','Áo dài cưới đỏ demo','Áo dài cưới demo màu đỏ, dùng làm mẫu trưng bày và tham khảo phối đồ.',1890000,1790000,'Gấm pha','Ôm vừa','FEMALE','apparel'),
('ao-dai-cuoi-nam-demo','trang-phuc-le-hoi-viet-demo','Áo dài lễ nam demo','Áo dài nam demo cho lễ cưới và sự kiện; phụ kiện không mặc định đi kèm.',1680000,1590000,'Gấm pha','Dáng suông','MALE','apparel'),
('ao-ba-ba-nam-demo','ao-ba-ba-demo','Áo bà ba nam','Áo bà ba demo lấy cảm hứng trang phục thường ngày Nam Bộ.',620000,569000,'Cotton','Dáng suông','MALE','apparel'),
('ao-ba-ba-nu-demo','ao-ba-ba-demo','Áo bà ba nữ','Áo bà ba demo phom gọn, phối quần dài; ảnh chỉ mang tính tham khảo.',650000,599000,'Cotton pha','Vừa vặn','FEMALE','apparel'),
('ao-ba-ba-linen-demo','ao-ba-ba-demo','Áo bà ba vải nhẹ','Biến thể demo bằng vải nhẹ, phù hợp mặc thường ngày trong thời tiết ấm.',720000,669000,'Linen pha','Dáng suông','UNISEX','apparel'),
('ao-ba-ba-cham-demo','ao-ba-ba-demo','Áo bà ba sắc chàm','Mẫu demo phối sắc chàm lấy cảm hứng từ trang phục miền Nam.',740000,689000,'Cotton pha','Dáng suông','UNISEX','apparel'),
('khan-van-truyen-thong-demo','phu-kien-viet-truyen-thong-demo','Khăn vấn truyền thống','Phụ kiện demo để phối áo dài và Việt phục; màu có thể khác ảnh tham khảo.',320000,289000,'Vải dệt','Một cỡ','FEMALE','one'),
('khan-dong-nam-demo','phu-kien-viet-truyen-thong-demo','Khăn đóng nam','Phụ kiện demo phối áo dài nam hoặc áo ngũ thân.',350000,319000,'Vải dệt','Một cỡ','MALE','one'),
('khan-ran-nam-bo-demo','phu-kien-viet-truyen-thong-demo','Khăn rằn Nam Bộ','Khăn demo lấy cảm hứng khăn rằn, dùng làm phụ kiện phối đồ.',180000,159000,'Cotton','Một cỡ','UNISEX','one'),
('guoc-go-truyen-thong-demo','phu-k \m ien-viet-truyen-thong-demo','Guốc gỗ kiểu truyền thống','Guốc demo lấy cảm hứng phụ kiện xưa; kích cỡ chọn theo bảng size.',490000,449000,'Gỗ, quai vải','Đế thấp','UNISEX','footwear'),
('dai-vai-phoi-viet-phuc-demo','phu-kien-viet-truyen-thong-demo','Đai vải phối Việt phục','Đai vải demo phối áo dài, áo tấc hoặc trang phục lấy cảm hứng truyền thống.',260000,229000,'Vải dệt','Có thể điều chỉnh','UNISEX','one');

-- Remove the previously retired item only when no cart/order references it.
DELETE FROM products p WHERE p.slug='viet-phuc-tu-than-demo'
 AND NOT EXISTS (SELECT 1 FROM cart_items ci WHERE ci.product_id=p.id)
 AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.product_id=p.id);
UPDATE products SET status='INACTIVE', is_featured=FALSE, is_new=FALSE, updated_at=NOW()
WHERE slug='viet-phuc-tu-than-demo';

INSERT INTO products (brand_id,category_id,name,slug,short_description,description,base_price,sale_price,material,fit,gender,status,is_featured,is_new)
SELECT b.id,c.id,s.name,s.slug,s.description,s.description,s.base_price,s.sale_price,s.material,s.fit,s.gender,'ACTIVE',FALSE,TRUE
FROM vietnamese_traditional_seed s JOIN brands b ON b.slug='di-san-viet-demo' JOIN categories c ON c.slug=s.category_slug
ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name,category_id=EXCLUDED.category_id,short_description=EXCLUDED.short_description,description=EXCLUDED.description,base_price=EXCLUDED.base_price,sale_price=EXCLUDED.sale_price,material=EXCLUDED.material,fit=EXCLUDED.fit,gender=EXCLUDED.gender,status='ACTIVE';

INSERT INTO product_variants (product_id,sku,color,size,stock_qty,price_adjustment,is_active)
SELECT p.id,'TV-'||upper(substr(md5(s.slug),1,8))||'-'||clr.code||'-'||sz.label,clr.name,sz.label,
       8+((row_number() OVER(PARTITION BY p.id ORDER BY clr.code,sz.label)::integer*3)%18),0,TRUE
FROM vietnamese_traditional_seed s JOIN products p ON p.slug=s.slug
CROSS JOIN (VALUES ('RED','Đỏ'),('IND','Chàm'),('IVO','Trắng ngà')) AS clr(code,name)
CROSS JOIN LATERAL unnest(CASE s.size_group WHEN 'one' THEN ARRAY['OS'] WHEN 'footwear' THEN ARRAY['36','37','38','39','40','41'] ELSE ARRAY['S','M','L','XL'] END) AS sz(label)
ON CONFLICT (sku) DO UPDATE SET color=EXCLUDED.color,size=EXCLUDED.size,stock_qty=EXCLUDED.stock_qty,is_active=TRUE;

INSERT INTO collection_products (collection_id,product_id,display_order)
SELECT c.id,p.id,row_number() OVER(ORDER BY p.slug)::integer FROM products p
JOIN collections c ON c.slug='net-viet-truyen-thong-demo'
WHERE p.slug IN (SELECT slug FROM vietnamese_traditional_seed)
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE vietnamese_traditional_images ON COMMIT DROP AS
SELECT p.id AS product_id,
       (SELECT v.id FROM product_variants v WHERE v.product_id=p.id ORDER BY v.is_active DESC,v.id LIMIT 1) AS variant_id,
       CASE p.slug
         WHEN 'viet-phuc-ao-tac-nu-demo' THEN '/assets/products/vietnamese-traditional/ao-tac/viet-phuc-ao-tac-nu-demo/main.webp'
         WHEN 'ao-ba-ba-nu-demo' THEN '/assets/products/vietnamese-traditional/ao-ba-ba/ao-ba-ba-nu-demo/main.webp'
         WHEN 'ao-dai-nu-co-dien-demo' THEN '/assets/products/vietnamese-traditional/ao-dai/ao-dai-nu-co-dien-demo/main.webp'
         ELSE CASE c.slug
           WHEN 'viet-phuc-co-demo' THEN 'https://upload.wikimedia.org/wikipedia/commons/thumb/6/62/Rio_m%C3%A3_ch%C3%A2u_%C3%A1o_t%E1%BA%A5c.jpg/500px-Rio_m%C3%A3_ch%C3%A2u_%C3%A1o_t%E1%BA%A5c.jpg'
           WHEN 'ao-ba-ba-demo' THEN 'https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Vietnam_2011.jpg/500px-Vietnam_2011.jpg'
           ELSE 'https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/%C3%81o_d%C3%A0i%2C_H%E1%BB%93_G%C6%B0%C6%A1m.jpg/500px-%C3%81o_d%C3%A0i%2C_H%E1%BB%93_G%C6%B0%C6%A1m.jpg'
         END
       END AS image_url,
       CASE p.slug
         WHEN 'viet-phuc-ao-tac-nu-demo' THEN 'Ảnh áo tấc: Ptdtch, CC BY-SA 4.0.'
         WHEN 'ao-ba-ba-nu-demo' THEN 'Ảnh áo bà ba: 4bangoai, CC BY-SA 4.0.'
         WHEN 'ao-dai-nu-co-dien-demo' THEN 'Ảnh áo dài: tq2cute, CC BY 2.0.'
         ELSE CASE c.slug
           WHEN 'viet-phuc-co-demo' THEN 'Ảnh tham khảo áo tấc. Ptdtch, CC BY-SA 4.0.'
           WHEN 'ao-ba-ba-demo' THEN 'Ảnh tham khảo áo bà ba. 4bangoai, CC BY-SA 4.0.'
           ELSE 'Ảnh tham khảo áo dài/phong cách Việt phục. tq2cute, CC BY 2.0.'
         END
       END AS alt_text
FROM products p JOIN categories c ON c.id=p.category_id
WHERE p.slug IN (SELECT slug FROM vietnamese_traditional_seed);

-- Keep and refresh one representative image per product; never delete the rest.
UPDATE product_images i SET variant_id=src.variant_id,image_url=src.image_url,alt_text=src.alt_text,is_primary=TRUE,display_order=1
FROM vietnamese_traditional_images src
WHERE i.id=(SELECT chosen.id FROM product_images chosen WHERE chosen.product_id=src.product_id ORDER BY chosen.is_primary DESC,chosen.display_order,chosen.id LIMIT 1);

-- Give every catalog product one representative image, using any available variant.
INSERT INTO product_images (product_id,variant_id,image_url,alt_text,is_primary,display_order)
SELECT src.product_id,src.variant_id,src.image_url,src.alt_text,TRUE,1
FROM vietnamese_traditional_images src
WHERE NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id=src.product_id);

COMMIT;

