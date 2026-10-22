# Arrow catalog repair

The Arrow product seed values were restored from commit `8e9fc73d360043e10418d1c60d947bfb04fd13c4` (product name, slug, demo prices, gender/flags, two variant SKUs and their sizes/stock, category, and image-to-M variant relationship). The corrected long image path is present in the following historical seed revision and matches the local manifest's dataset ID `18896` and SHA-256.

## Backup and inspect

Before a production write, save a full PostgreSQL backup with a locally installed PostgreSQL client:

```powershell
$env:PGPASSWORD = 'fashion'
pg_dump -h 127.0.0.1 -U fashion -d fashion -Fc -f database/backups/fashion-before-arrow-image-repair.dump
```

Use a protected backup directory and a database-specific credential in non-demo environments. Confirm the output file exists and is non-empty before continuing. The migration itself checks product identity, both historical variant tuples, SKU ownership and exactly one primary image attached to the M variant. It changes only `product_images.image_url` and `alt_text`; IDs and foreign keys remain unchanged. It is safe to rerun after success.

```powershell
$env:PGPASSWORD = 'fashion'
psql -h 127.0.0.1 -U fashion -d fashion -v ON_ERROR_STOP=1 -f database/migrations/20261022_repair_arrow_product_image.sql
```

The migration raises an exception before writing if the live rows do not match the verified snapshot.

## Restore if required

Restore the full backup to a separate database for review:

```powershell
createdb -h 127.0.0.1 -U fashion fashion_arrow_recovery
pg_restore -h 127.0.0.1 -U fashion -d fashion_arrow_recovery database/backups/fashion-before-arrow-image-repair.dump
```

Do not restore over a live database without a separately reviewed recovery plan.
