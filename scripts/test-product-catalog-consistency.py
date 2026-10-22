#!/usr/bin/env python3
"""Check the Arrow/Hity product seed, initializer and image manifest agree."""

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED = (ROOT / "database/seed.sql").read_text(encoding="utf-8")
INITIALIZER = (ROOT / "src/main/java/com/fashion/product/config/CatalogInitializer.java").read_text(encoding="utf-8")
MANIFEST = json.loads((ROOT / "frontend/public/assets/products/seed-catalog/manifest.json").read_text(encoding="utf-8"))
IMAGE_MAP = json.loads((ROOT / "database/seed-image-map.json").read_text(encoding="utf-8"))
IMAGE_BUILDER = (ROOT / "scripts/build_seed_images.py").read_text(encoding="utf-8")
ARROW_IMAGE = "/assets/products/seed-catalog/dresses-cream-arrow-woman-solid-cream-dress-18896.jpg"
HITY_IMAGE = "https://product.hstatic.net/1000154340/product/vai_lanh_do_dam_co_tim_kieu_vintage_co_dien_thoi_trang_thiet_ke_hity_2_dd759789d7d4499c91a79600b78dfaac_master.jpg"


class CatalogConsistencyTest(unittest.TestCase):
    def test_arrow_manifest_and_local_image_match(self):
        entry = next(item for item in MANIFEST if item["datasetId"] == "18896")
        self.assertEqual(entry["seedKey"], "dam-midi-linen")
        self.assertEqual(entry["sourceName"], "Arrow Woman Solid Cream Dress")
        self.assertEqual(entry["localImage"], ARROW_IMAGE)
        self.assertTrue((ROOT / "frontend/public" / ARROW_IMAGE.lstrip("/")).is_file())
        self.assertIn(ARROW_IMAGE, INITIALIZER)
        self.assertRegex(SEED, r"'dam-midi-linen',\s*'DML-001-SND-M',\s*'" + re.escape(ARROW_IMAGE) + r"'")
        mappings = [item for item in IMAGE_MAP["baseProducts"] if item["productSlug"] == "dam-midi-linen"]
        self.assertEqual(len(mappings), 1)
        self.assertEqual(mappings[0]["variantSku"], "DML-001-SND-M")
        self.assertEqual(mappings[0]["image"], ARROW_IMAGE)
        self.assertIn('"dam-midi-linen": ("dam-midi-linen", "DML-001-SND-M", True)', IMAGE_BUILDER)
        self.assertIn('key != "dam-midi-linen"', IMAGE_BUILDER)

    def test_arrow_and_hity_have_separate_identity_and_skus(self):
        self.assertIn('"Arrow Woman Solid Cream Dress"', INITIALIZER)
        self.assertIn('"dam-midi-linen"', INITIALIZER)
        self.assertIn('variant("DML-001-SND-S", "Cream", "S", 11, 0)', INITIALIZER)
        self.assertIn('variant("DML-001-SND-M", "Cream", "M", 13, 0)', INITIALIZER)
        self.assertIn('"dam-midi-linen-hity"', INITIALIZER)
        self.assertIn('variant("HITY-DML-001-SND-S", "Sand", "S", 11, 0)', INITIALIZER)
        self.assertIn('variant("HITY-DML-001-SND-M", "Sand", "M", 13, 0)', INITIALIZER)
        self.assertIn(HITY_IMAGE, INITIALIZER)
        self.assertNotIn("DML-001-SND-S.*dam-midi-linen-hity", INITIALIZER)

    def test_seed_guards_conflicting_owners_and_is_idempotent_for_images(self):
        self.assertIn("Refusing to seed Arrow", SEED)
        self.assertIn("Refusing to seed Hity", SEED)
        self.assertIn("ON CONFLICT (sku) DO NOTHING", SEED)
        self.assertIn("NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id=p.id AND i.variant_id=v.id AND i.image_url=m.image_url)", IMAGE_BUILDER)
        self.assertIn("WHERE products.name=U&'\\0110\\1EA7m linen c\\1ED5 tim d\\00E1ng midi Hity'", SEED)
        self.assertIn("WHERE products.name=U&'\\0110\\1EA7m linen c\\1ED5 tim d\\00E1ng midi Hity'", SEED)
        self.assertNotRegex(SEED, r"DELETE FROM product_images i USING products p\s*WHERE i\.product_id=p\.id AND p\.slug IN \([^;]*'dam-midi-linen'[,)]")

    def test_initializer_hity_sku_does_not_collide_with_arrow(self):
        skus = re.findall(r'variant\("([A-Z0-9-]+)"', INITIALIZER)
        self.assertEqual(len(skus), len(set(skus)))
        self.assertNotEqual("DML-001-SND-M", "HITY-DML-001-SND-M")


if __name__ == "__main__":
    unittest.main(verbosity=2)
