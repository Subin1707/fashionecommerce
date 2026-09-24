# Demo data generators

These scripts add deterministic demo data without deleting existing rows. They are intended to run only after PostgreSQL schema initialization and application migrations are stable.

## Install

```powershell
python -m pip install -r database/requirements.txt
```

## Run all generators

```powershell
python database/generate_all.py
```

The order is collections, products and variants, users, then reviews. Individual scripts are also available:

```powershell
python database/generate_collections.py
python database/generate_products.py
python database/generate_users.py
python database/generate_reviews.py
```

Targets are minimum row counts:

- 50 collections
- 3,000 products
- 15,000 variants
- 10 users
- 30,000 reviews

The scripts use `FASHION_DB_HOST`, `FASHION_DB_PORT`, `FASHION_DB_NAME`, `FASHION_DB_USER`, and `FASHION_DB_PASSWORD` when supplied; otherwise they use the local development PostgreSQL settings. Existing rows are preserved and duplicate natural keys are ignored.

`generate_reviews.py` requires users and products to exist first. Demo users use `FASHION_DEMO_PASSWORD_HASH`; set it to a valid bcrypt hash before using generated accounts to log in.
