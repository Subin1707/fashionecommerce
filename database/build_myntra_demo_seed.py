"""Build an idempotent 1,000-row demo catalog from the supplied Myntra sample ZIP.

The source dataset is used only as sample catalog content. Prices and stock are
synthetic; no commercial offer or original variant information is implied.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import re
import shutil
import zipfile
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "frontend" / "public" / "assets" / "products" / "myntra-sample"
SQL_PATH = ROOT / "database" / "seed_myntra_demo.sql"
MAIN_SEED_PATH = ROOT / "database" / "seed.sql"
MANIFEST_PATH = ASSET_DIR / "manifest.json"
MAIN_SEED_MARKER = "-- BEGIN GENERATED FASHION DATASET SAMPLE"

CATEGORIES = {
    "apparel-tops": ("Dataset Apparel - Tops", "Áo và trang phục phần trên (mẫu dataset)"),
    "apparel-bottoms": ("Dataset Apparel - Bottoms", "Quần và trang phục phần dưới (mẫu dataset)"),
    "apparel-outerwear": ("Dataset Apparel - Outerwear", "Áo khoác và đồ giữ ấm (mẫu dataset)"),
    "apparel-dresses": ("Dataset Apparel - Dresses", "Đầm và váy liền (mẫu dataset)"),
    "apparel-ethnic": ("Dataset Apparel - Ethnic", "Trang phục ethnic của dataset (không khẳng định là Việt phục)"),
    "footwear": ("Dataset Footwear", "Giày dép (mẫu dataset)"),
    "bags": ("Dataset Bags", "Túi và ba lô (mẫu dataset)"),
    "accessories": ("Dataset Accessories", "Phụ kiện (mẫu dataset)"),
}

TYPE_TO_CATEGORY = {
    "tops": "apparel-tops", "tshirts": "apparel-tops", "shirts": "apparel-tops",
    "polos": "apparel-tops", "kurtas": "apparel-ethnic", "kurtis": "apparel-ethnic",
    "tunics": "apparel-tops", "waistcoats": "apparel-outerwear",
    "sweatshirts": "apparel-outerwear", "sweaters": "apparel-outerwear",
    "jackets": "apparel-outerwear", "blazers": "apparel-outerwear",
    "shrugs": "apparel-outerwear", "rain jackets": "apparel-outerwear",
    "jeans": "apparel-bottoms", "trousers": "apparel-bottoms", "shorts": "apparel-bottoms",
    "track pants": "apparel-bottoms", "leggings": "apparel-bottoms", "skirts": "apparel-bottoms",
    "capris": "apparel-bottoms", "dresses": "apparel-dresses", "jumpsuits": "apparel-dresses",
    "rompers": "apparel-dresses", "sarees": "apparel-ethnic", "lehenga choli": "apparel-ethnic",
    "sherwanis": "apparel-ethnic", "ethnic dresses": "apparel-ethnic",
    "casual shoes": "footwear", "sports shoes": "footwear", "formal shoes": "footwear",
    "heels": "footwear", "flats": "footwear", "sandals": "footwear", "flip flops": "footwear",
    "boots": "footwear", "handbags": "bags", "backpacks": "bags", "clutches": "bags",
    "wallets": "accessories", "belts": "accessories", "watches": "accessories",
    "sunglasses": "accessories", "eyewear": "accessories", "socks": "accessories",
    "caps": "accessories", "scarves": "accessories", "jewellery sets": "accessories",
    "earrings": "accessories", "necklaces": "accessories", "bracelets": "accessories",
    "rings": "accessories", "ties": "accessories", "hair accessory": "accessories",
    "headwear": "accessories",
}


def sql(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def safe_slug(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")


def gender_code(value: str) -> str:
    return {"Men": "MALE", "Boys": "MALE", "Women": "FEMALE", "Girls": "FEMALE"}.get(value, "UNISEX")


def stable_price(dataset_id: str, category: str) -> int:
    digest = int(hashlib.sha256(dataset_id.encode()).hexdigest()[:8], 16)
    limits = {
        "footwear": (299_000, 1_500_000), "bags": (199_000, 900_000),
        "accessories": (79_000, 700_000),
    }
    low, span = limits.get(category, (149_000, 1_000_000))
    return round((low + digest % span) * 0.7 / 1_000) * 1_000


def load_rows(archive: zipfile.ZipFile) -> list[dict[str, str]]:
    archive_names = set(archive.namelist())
    with archive.open("styles.csv") as source:
        rows = list(csv.DictReader(io.TextIOWrapper(source, encoding="utf-8-sig", newline="")))
    eligible: dict[str, list[dict[str, str]]] = defaultdict(list)
    for row in rows:
        category = TYPE_TO_CATEGORY.get((row.get("articleType") or "").strip().lower())
        if category and row.get("masterCategory") in {"Apparel", "Footwear", "Accessories"}:
            dataset_id = (row.get("id") or "").strip()
            title = (row.get("productDisplayName") or "").strip()
            if dataset_id.isdigit() and title and f"images/{dataset_id}.jpg" in archive_names:
                row["_category"] = category
                eligible[category].append(row)

    selected: list[dict[str, str]] = []
    selected_titles: set[str] = set()
    selected_image_hashes: set[str] = set()
    image_hash_cache: dict[str, str] = {}
    for category in CATEGORIES:
        candidates = sorted(eligible[category], key=lambda item: (int(item["id"]), item["productDisplayName"]))
        # Spread selection deterministically over the available source rows.
        quota = 125
        if len(candidates) < quota:
            raise ValueError(f"Only {len(candidates)} eligible source rows for {category}; need {quota}.")
        step = len(candidates) / quota
        ordered = [candidates[int(i * step)] for i in range(quota)]
        ordered.extend(candidates)
        category_selected = 0
        seen_ids: set[str] = set()
        for candidate in ordered:
            title_key = candidate["productDisplayName"].strip().casefold()
            if candidate["id"] in seen_ids or title_key in selected_titles:
                continue
            dataset_id = candidate["id"]
            digest = image_hash_cache.get(dataset_id)
            if digest is None:
                digest = hashlib.sha256(archive.read(f"images/{dataset_id}.jpg")).hexdigest()
                image_hash_cache[dataset_id] = digest
            if digest in selected_image_hashes:
                continue
            selected.append(candidate)
            selected_titles.add(title_key)
            seen_ids.add(candidate["id"])
            selected_image_hashes.add(digest)
            category_selected += 1
            if category_selected == quota:
                break
        if category_selected != quota:
            raise ValueError(f"Only {category_selected} unique titled rows for {category}; need {quota}.")
    if len({row["id"] for row in selected}) != 1000:
        raise ValueError("Selected dataset IDs are not unique.")
    return selected


def build(archive_path: Path) -> None:
    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive_path) as archive:
        rows = load_rows(archive)
        image_names = {
            row["id"]: f"{safe_slug(row.get('articleType') or row['_category'])}-{safe_slug(row['productDisplayName'])[:90]}-{row['id']}.jpg"
            for row in rows
        }
        expected_images = set(image_names.values())
        for old_image in ASSET_DIR.glob("*.jpg"):
            if old_image.name not in expected_images:
                old_image.unlink()
        manifest = []
        values = []
        for row in rows:
            dataset_id = row["id"]
            category = row["_category"]
            title = row["productDisplayName"].strip()
            image_rel = f"/assets/products/myntra-sample/{image_names[dataset_id]}"
            local_image = ROOT / "frontend" / "public" / image_rel.lstrip("/")
            with archive.open(f"images/{dataset_id}.jpg") as source, local_image.open("wb") as target:
                shutil.copyfileobj(source, target)

            description = (
                f"Mẫu catalog từ Fashion Product Images Dataset (ID {dataset_id}; "
                f"{row.get('articleType', '')}; màu {row.get('baseColour') or 'không rõ'}; "
                f"mùa {row.get('season') or 'không rõ'}; mục đích {row.get('usage') or 'không rõ'}). "
                "Giá, tồn kho và lựa chọn size là dữ liệu demo được tạo cho Fashion Shop, "
                "không phải thông tin bán hàng gốc."
            )
            base_price = stable_price(dataset_id, category)
            sizes = "one" if category in {"bags", "accessories"} else "shoes" if category == "footwear" else "clothes"
            color = (row.get("baseColour") or "Assorted").strip()[:80]
            slug = f"myntra-sample-{dataset_id}"
            values.append(
                "(" + ",".join([
                    sql(dataset_id), sql(slug), sql(title[:200]), sql(category), sql(description),
                    str(base_price), sql(gender_code(row.get("gender", ""))), sql(color),
                    sql(row.get("articleType", "")[:100]), sql(row.get("season", "")[:50]),
                    sql(row.get("usage", "")[:50]), sql(sizes), sql(image_rel),
                ]) + ")"
            )
            manifest.append({
                "datasetId": dataset_id, "slug": slug, "name": title, "category": category,
                "image": image_rel, "sourceArticleType": row.get("articleType"),
                "sourceColor": row.get("baseColour"), "sourceGender": row.get("gender"),
                "sourceSeason": row.get("season"), "sourceUsage": row.get("usage"),
                "demoBasePriceVnd": base_price,
            })

    lines = [
        "-- Idempotent 1,000-product sample catalog generated from supplied archive.zip.",
        "-- Run after database/seed.sql and migrations. This is demo data, not a live product feed.",
        "-- Original source fields are preserved; price/stock/size options are synthetic demo values.",
        "BEGIN;",
        "INSERT INTO brands (name,slug,description,is_active) VALUES",
        "('Dataset Sample','dataset-sample-myntra','Thương hiệu kỹ thuật cho dữ liệu catalog demo; brand gốc không có trường riêng trong nguồn.',TRUE)",
        "ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,is_active=TRUE;",
        "INSERT INTO categories (name,slug,description,is_active) VALUES",
    ]
    lines.append(",\n".join(f"({sql(name)},{sql('dataset-' + slug)},{sql(description)},TRUE)" for slug, (name, description) in CATEGORIES.items()))
    lines.append("ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,is_active=TRUE;")
    lines.extend([
        "INSERT INTO collections (name,slug,description,season,is_featured) VALUES",
        "('Fashion Dataset Samples','fashion-dataset-samples','1.000 sản phẩm mẫu từ bộ dữ liệu công khai; giá và tồn kho là dữ liệu demo.','ALL_SEASON',FALSE)",
        "ON CONFLICT (slug) DO UPDATE SET description=EXCLUDED.description,is_featured=FALSE;",
        "CREATE TEMP TABLE fashion_dataset_seed (dataset_id text PRIMARY KEY, slug text UNIQUE, product_name text, category_slug text, description text, base_price numeric(12,2), gender text, color text, article_type text, season text, usage text, size_group text, image_url text) ON COMMIT DROP;",
        "INSERT INTO fashion_dataset_seed VALUES",
        ",\n".join(values) + ";",
        "INSERT INTO products (brand_id,category_id,name,slug,short_description,description,base_price,sale_price,material,fit,gender,status,is_featured,is_new)",
        "SELECT b.id,c.id,s.product_name,s.slug,('Mẫu dataset: '||COALESCE(s.article_type,'fashion')||' · '||COALESCE(NULLIF(s.color,'Assorted'),'màu chưa rõ')||'. Giá và tồn kho chỉ để demo.'),s.description,s.base_price,NULL,NULL,NULL,s.gender,'ACTIVE',FALSE,FALSE",
        "FROM fashion_dataset_seed s JOIN brands b ON b.slug='dataset-sample-myntra' JOIN categories c ON c.slug='dataset-'||s.category_slug",
        "ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name,category_id=EXCLUDED.category_id,short_description=EXCLUDED.short_description,description=EXCLUDED.description,base_price=EXCLUDED.base_price,sale_price=NULL,gender=EXCLUDED.gender,status='ACTIVE',updated_at=NOW();",
        "INSERT INTO product_variants (product_id,sku,color,size,stock_qty,price_adjustment,is_active)",
        "SELECT p.id,'MDS-'||s.dataset_id||'-'||z.size_label,s.color,z.size_label,10,0,TRUE",
        "FROM fashion_dataset_seed s JOIN products p ON p.slug=s.slug",
        "CROSS JOIN LATERAL unnest(CASE s.size_group WHEN 'one' THEN ARRAY['OS'] WHEN 'shoes' THEN ARRAY['39','40','41'] ELSE ARRAY['S','M','L','XL'] END) AS z(size_label)",
        "ON CONFLICT (sku) DO UPDATE SET color=EXCLUDED.color,size=EXCLUDED.size,stock_qty=10,is_active=TRUE,updated_at=NOW();",
        "-- All sample product images are local files under frontend/public/assets/products.",
        "DELETE FROM product_images i USING products p",
        "WHERE i.product_id=p.id AND p.slug IN (SELECT slug FROM fashion_dataset_seed);",
        "INSERT INTO product_images (product_id,variant_id,image_url,alt_text,is_primary,display_order)",
        "SELECT p.id,(SELECT v.id FROM product_variants v WHERE v.product_id=p.id ORDER BY v.id LIMIT 1),s.image_url,('Dataset sample image: '||s.product_name),TRUE,1",
        "FROM fashion_dataset_seed s JOIN products p ON p.slug=s.slug",
        ";",
        "INSERT INTO collection_products (collection_id,product_id,display_order)",
        "SELECT c.id,p.id,row_number() OVER (ORDER BY p.slug)::integer FROM products p JOIN collections c ON c.slug='fashion-dataset-samples'",
        "WHERE p.slug IN (SELECT slug FROM fashion_dataset_seed) ON CONFLICT DO NOTHING;",
        "COMMIT;",
        "",
    ])
    generated_sql = "\n".join(lines)
    SQL_PATH.write_text(generated_sql, encoding="utf-8")
    main_seed = MAIN_SEED_PATH.read_text(encoding="utf-8")
    if MAIN_SEED_MARKER in main_seed:
        main_seed = main_seed[:main_seed.index(MAIN_SEED_MARKER)].rstrip() + "\n"
    main_seed += (
        "\n" + MAIN_SEED_MARKER + "\n"
        "-- Re-running database/seed.sql re-upserts this isolated demo sample catalog.\n"
        + generated_sql
        + "-- END GENERATED FASHION DATASET SAMPLE\n"
    )
    MAIN_SEED_PATH.write_text(main_seed, encoding="utf-8")
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Selected rows: {len(rows)}")
    print(f"SQL seed: {SQL_PATH.relative_to(ROOT)}")
    print(f"Integrated main seed: {MAIN_SEED_PATH.relative_to(ROOT)}")
    print(f"Images and manifest: {ASSET_DIR.relative_to(ROOT)}")
    print(f"Image bytes: {sum((ROOT / 'frontend' / 'public' / item['image'].lstrip('/')).stat().st_size for item in manifest)}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=Path, help="Path to the supplied archive.zip")
    args = parser.parse_args()
    build(args.archive)


if __name__ == "__main__":
    main()
