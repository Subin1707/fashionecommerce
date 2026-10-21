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

The Vietnamese demo catalog seed was removed.

## Optional 1,000-item fashion dataset sample

The supplied `archive.zip` can be converted into a deterministic, idempotent demo seed with 1,000 distinct product IDs and 1,000 distinct display names. It copies the selected 60×80 source thumbnails into `frontend/public/assets/products/myntra-sample/`, creates `database/seed_myntra_demo.sql` plus an image/source manifest, and adds a marked generated block to `database/seed.sql`.

```powershell
python database/build_myntra_demo_seed.py E:\archive.zip
```

After schema initialization and application migrations, the usual `database/seed.sql` now includes this catalog. To apply or refresh only the sample block against an existing database, run:

```powershell
psql -U fashion -d fashion -v ON_ERROR_STOP=1 -f database/seed_myntra_demo.sql
```

The generated block does not delete existing rows. It creates separate dataset categories and a technical `Dataset Sample` brand. The source does not provide Fashion Shop prices, stock or size variants, so the seed generates clearly labeled demo values; do not treat them as real offers. The source thumbnails are 60×80 and unsuitable as production product photography. See `frontend/public/assets/products/myntra-sample/ATTRIBUTION.md` before distributing or deploying these sample assets.
