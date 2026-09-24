"""Generate about 10 demo user accounts without deleting existing users."""

from generate_data_common import PASSWORD_HASH, scalar, transaction

TARGET = 10


def generate_users() -> int:
    with transaction() as connection:
        role_id = connection.execute(
            "INSERT INTO roles (name) VALUES ('CUSTOMER') ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id"
        ).fetchone()[0]
        current = scalar(connection, "SELECT COUNT(*) FROM users")
        for offset in range(max(0, TARGET - current)):
            number = current + offset + 1
            connection.execute(
                """
                INSERT INTO users
                    (full_name, email, password, phone, address, avatar, role_id, status)
                VALUES (%s, %s, %s, %s, %s, NULL, %s, 'ACTIVE')
                ON CONFLICT (email) DO NOTHING
                """,
                (
                    f"Demo Customer {number:04d}",
                    f"demo.customer.{number:04d}@example.com",
                    PASSWORD_HASH,
                    f"090{number % 10000000:07d}",
                    f"Demo address {number:04d}",
                    role_id,
                ),
            )
        users = connection.execute("SELECT id FROM users ORDER BY id").fetchall()
        for user_id in users:
            connection.execute(
                """
                INSERT INTO user_style_profiles
                    (user_id, preferred_styles, preferred_colors, favorite_brands,
                     top_size, bottom_size, shoe_size, body_shape, style_notes)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (user_id) DO NOTHING
                """,
                (
                    user_id[0], "CASUAL,OFFICE", "BLACK,WHITE,NAVY", "Demo Brand",
                    ("S", "M", "L", "XL")[user_id[0] % 4],
                    ("S", "M", "L", "XL")[user_id[0] % 4], "42", "REGULAR", "Generated demo profile",
                ),
            )
        return scalar(connection, "SELECT COUNT(*) FROM users")


if __name__ == "__main__":
    print(f"users={generate_users()}")
