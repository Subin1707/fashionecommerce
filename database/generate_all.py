"""Run the demo-data generators in foreign-key order."""

from generate_collections import generate_collections
from generate_products import generate_products
from generate_reviews import generate_reviews
from generate_users import generate_users


if __name__ == "__main__":
    print(f"collections={generate_collections()}")
    products, variants = generate_products()
    print(f"products={products} variants={variants}")
    print(f"users={generate_users()}")
    print(f"reviews={generate_reviews()}")
