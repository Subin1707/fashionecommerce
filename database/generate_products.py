"""Generate at least 3,000 products and 15,000 variants without deleting data."""

from __future__ import annotations

from generate_data_common import scalar, slug, transaction

PRODUCT_TARGET = 3_000
VARIANT_TARGET = 15_000


def generate_products() -> tuple[int, int]:
    with transaction() as connection:
        brands = []
        for index in range(1, 21):
            name = f"Demo Brand {index:02d}"
            brands.append(
                connection.execute(
                    """
                    INSERT INTO brands (name, slug, description, is_active)
                    VALUES (%s, %s, %s, TRUE)
                    ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name
                    RETURNING id
                    """,
                    (name, slug(name), "Generated demo brand"),
                ).fetchone()[0]
            )

        categories = []
        for index, name in enumerate(("TOP", "BOTTOM", "DRESS", "OUTERWEAR", "ACCESSORIES"), 1):
            categories.append(
                connection.execute(
                    """
                    INSERT INTO categories (name, slug, description, is_active)
                    VALUES (%s, %s, %s, TRUE)
                    ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name
                    RETURNING id
                    """,
                    (name, slug(name), "Generated demo category"),
                ).fetchone()[0]
            )

        current = scalar(connection, "SELECT COUNT(*) FROM products")
        needed = max(0, PRODUCT_TARGET - current)
        for offset in range(needed):
            number = current + offset + 1
            name = f"Demo Fashion Product {number:04d}"
            product_id = connection.execute(
                """
                INSERT INTO products
                    (brand_id, category_id, name, slug, short_description, description,
                     base_price, sale_price, material, fit, gender, status, is_featured, is_new)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 'ACTIVE', FALSE, TRUE)
                ON CONFLICT (slug) DO NOTHING
                RETURNING id
                """,
                (
                    brands[number % len(brands)], categories[number % len(categories)], name,
                    slug(name), "Generated demo product", "Generated for scale testing",
                    100000 + (number % 20) * 10000, 90000 + (number % 20) * 10000,
                    "Cotton", "Regular", "UNISEX",
                ),
            ).fetchone()
            if product_id:
                for variant_index, (size, color) in enumerate(
                    (("S", "BLACK"), ("M", "WHITE"), ("L", "NAVY"), ("XL", "GRAY"), ("M", "BEIGE"))
                ):
                    connection.execute(
                        """
                        INSERT INTO product_variants
                            (product_id, sku, color, size, stock_qty, price_adjustment, is_active)
                        VALUES (%s, %s, %s, %s, %s, 0, TRUE)
                        ON CONFLICT (sku) DO NOTHING
                        """,
                        (product_id[0], f"DEMO-{number:04d}-{variant_index + 1}", color, size, 10),
                    )

        return scalar(connection, "SELECT COUNT(*) FROM products"), scalar(
            connection, "SELECT COUNT(*) FROM product_variants"
        )


if __name__ == "__main__":
    products, variants = generate_products()
    print(f"products={products} variants={variants}")
