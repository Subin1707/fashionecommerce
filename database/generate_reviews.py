"""Generate at least 30,000 reviews with deterministic user/product pairs."""

from generate_data_common import scalar, transaction

TARGET = 30_000


def generate_reviews() -> int:
    with transaction() as connection:
        users = [row[0] for row in connection.execute("SELECT id FROM users ORDER BY id").fetchall()]
        products = [row[0] for row in connection.execute("SELECT id FROM products ORDER BY id").fetchall()]
        if not users or not products:
            raise RuntimeError("Generate users and products before reviews")

        current = scalar(connection, "SELECT COUNT(*) FROM reviews")
        rows = []
        pair_index = 0
        while current + len(rows) < TARGET:
            user_id = users[pair_index % len(users)]
            product_id = products[(pair_index * 17) % len(products)]
            rating = 3 + (pair_index % 3)
            rows.append((user_id, product_id, rating, f"Generated review {pair_index + 1}", "APPROVED"))
            pair_index += 1
            if len(rows) >= 2_000:
                for row in rows:
                    connection.execute(
                        """
                        INSERT INTO reviews (user_id, product_id, rating, comment, status)
                        VALUES (%s, %s, %s, %s, %s)
                        ON CONFLICT (user_id, product_id) DO NOTHING
                        """,
                        row,
                    )
                rows.clear()
        if rows:
            for row in rows:
                connection.execute(
                    """
                    INSERT INTO reviews (user_id, product_id, rating, comment, status)
                    VALUES (%s, %s, %s, %s, %s)
                    ON CONFLICT (user_id, product_id) DO NOTHING
                    """,
                    row,
                )
        return scalar(connection, "SELECT COUNT(*) FROM reviews")


if __name__ == "__main__":
    print(f"reviews={generate_reviews()}")
