"""Generate at least 50 collections without deleting existing rows."""

from generate_data_common import scalar, transaction

TARGET = 50


def generate_collections() -> int:
    with transaction() as connection:
        current = scalar(connection, "SELECT COUNT(*) FROM collections")
        for offset in range(max(0, TARGET - current)):
            number = current + offset + 1
            name = f"Demo Collection {number:03d}"
            connection.execute(
                """
                INSERT INTO collections
                    (name, description, season, is_featured, banner_url)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (name) DO NOTHING
                """,
                (name, "Generated demo collection", "ALL", number <= 5, None),
            )
        return scalar(connection, "SELECT COUNT(*) FROM collections")


if __name__ == "__main__":
    print(f"collections={generate_collections()}")
