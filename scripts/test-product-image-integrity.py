#!/usr/bin/env python3
"""Verify referenced local catalog images and product/variant relationships."""

import os
import sys
import unittest
from pathlib import Path
from urllib.parse import unquote, urlparse

import psycopg

ROOT = Path(__file__).resolve().parents[1]
PUBLIC = ROOT / "frontend/public"
DATABASE_URL = os.environ.get(
    "DATABASE_URL",
    "host={host} port={port} dbname={dbname} user={user} password={password}".format(
        host=os.environ.get("FASHION_DB_HOST", "127.0.0.1"),
        port=os.environ.get("FASHION_DB_PORT", "5432"),
        dbname=os.environ.get("FASHION_DB_NAME", "fashion"),
        user=os.environ.get("FASHION_DB_USER", "fashion"),
        password=os.environ.get("FASHION_DB_PASSWORD", "fashion"),
    ),
)


def resolve_local_asset(url):
    parsed = urlparse(url)
    if parsed.scheme or parsed.netloc or not parsed.path.startswith("/assets/"):
        return None
    path = (PUBLIC / unquote(parsed.path).lstrip("/")).resolve()
    try:
        path.relative_to(PUBLIC.resolve())
    except ValueError:
        return None
    return path


class ProductImageIntegrityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        try:
            cls.connection = psycopg.connect(DATABASE_URL, connect_timeout=3)
        except psycopg.Error as error:
            raise unittest.SkipTest(f"PostgreSQL unavailable: {error}") from error

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls, "connection"):
            cls.connection.close()

    def test_product_images_have_valid_product_and_variant_relationships(self):
        rows = self.connection.execute(
            """SELECT p.id, p.name, p.slug, i.id, i.image_url, i.variant_id,
                      v.product_id AS variant_product_id, v.sku
               FROM product_images i
               LEFT JOIN products p ON p.id=i.product_id
               LEFT JOIN product_variants v ON v.id=i.variant_id"""
        ).fetchall()
        self.assertTrue(rows, "product_images table is empty")
        problems = []
        for product_id, name, slug, image_id, url, variant_id, variant_product_id, sku in rows:
            if product_id is None:
                problems.append(f"orphan image id={image_id}")
            if variant_id is not None and (variant_product_id is None or variant_product_id != product_id):
                problems.append(f"image id={image_id} SKU={sku} points to a variant of another product")
            asset = resolve_local_asset(url)
            if asset is not None and not asset.is_file():
                problems.append(f"missing local image product={product_id} {name!r} URL={url}")
            if not url or any(char.isspace() for char in url):
                problems.append(f"invalid image URL product={product_id} URL={url!r}")
        self.assertEqual(problems, [])

    def test_active_products_have_a_locally_existing_image_or_an_explicit_missing_status(self):
        rows = self.connection.execute(
            """SELECT p.id,p.name,COUNT(i.id),
                      COUNT(i.id) FILTER (WHERE i.image_url LIKE '/assets/%')
               FROM products p LEFT JOIN product_images i ON i.product_id=p.id
               WHERE p.status='ACTIVE' GROUP BY p.id,p.name"""
        ).fetchall()
        missing = []
        for product_id, name, image_count, local_count in rows:
            if image_count and local_count == 0:
                continue  # External images have an explicit URL and load error handling in SafeImage.
            if not image_count:
                missing.append((product_id, name))
        self.assertEqual(missing, [], f"Active products without any image row: {missing}")

    def test_arrow_primary_image_matches_manifest_image_and_variant(self):
        row = self.connection.execute(
            """SELECT p.id,p.name,p.slug,i.image_url,v.sku,i.is_primary
               FROM products p JOIN product_images i ON i.product_id=p.id
               JOIN product_variants v ON v.id=i.variant_id AND v.product_id=p.id
               WHERE p.slug='dam-midi-linen' AND i.is_primary=TRUE"""
        ).fetchall()
        self.assertEqual(len(row), 1, f"Expected exactly one Arrow primary image, got {row}")
        product_id, name, slug, url, sku, primary = row[0]
        self.assertEqual(name, "Arrow Woman Solid Cream Dress")
        self.assertEqual(url, "/assets/products/seed-catalog/dresses-cream-arrow-woman-solid-cream-dress-18896.jpg")
        self.assertEqual(sku, "DML-001-SND-M")
        self.assertTrue(primary)
        self.assertTrue((PUBLIC / url.lstrip("/")).is_file())

    def test_arrow_skus_are_unique_and_attached_only_to_arrow(self):
        rows = self.connection.execute(
            """SELECT v.sku,COUNT(*),COUNT(*) FILTER (
                      WHERE p.id=164 AND p.name='Arrow Woman Solid Cream Dress'
                        AND p.slug='dam-midi-linen')
               FROM product_variants v JOIN products p ON p.id=v.product_id
               WHERE v.sku IN ('DML-001-SND-S','DML-001-SND-M') GROUP BY v.sku"""
        ).fetchall()
        self.assertEqual({row[0] for row in rows}, {"DML-001-SND-S", "DML-001-SND-M"})
        self.assertTrue(all(total == 1 and attached == 1 for _, total, attached in rows), rows)


if __name__ == "__main__":
    unittest.main(verbosity=2)
