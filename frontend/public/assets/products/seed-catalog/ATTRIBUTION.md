# Seed catalog image mapping

These local sample images are selected from the user-supplied Fashion Product Images archive. The source row ID, display name, article type, color, and SHA-256 for each image are recorded in `manifest.json`.

The mappings in `database/seed.sql` keep product groups and approximate color aligned: T-shirt/white and black T-shirt/black, jeans/blue, jacket/black, dress/cream, jacket/blue, racerback top/white, handbag/black, and sports shoes/grey. These are sample references from the source catalog, not photographs of the Fashion Shop demo SKUs themselves.

The source ZIP did not include per-image authors or a license file. See [`../myntra-sample/ATTRIBUTION.md`](../myntra-sample/ATTRIBUTION.md) for the dataset listing, upstream rights caveat, and development-only scope.
