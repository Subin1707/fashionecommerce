#!/usr/bin/env python3
"""Build deterministic local image mappings and SQL from the checked-in manifests."""

from __future__ import annotations

import csv
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PUBLIC = ROOT / "frontend" / "public"
SEED_SQL = ROOT / "database" / "seed.sql"
MAP_JSON = ROOT / "database" / "seed-image-map.json"
SQL_OUT = ROOT / "database" / "seed-image-map.sql"
AUDIT_CSV = ROOT / "database" / "seed-image-audit.csv"
TARGET_SNAPSHOT = ROOT / "database" / "seed-image-targets.json"
BASE_MANIFEST = PUBLIC / "assets/products/seed-catalog/manifest.json"
SAMPLE_MANIFEST = PUBLIC / "assets/products/myntra-sample/manifest.json"

BASE = {
    "ao-thun-basic-primary": ("ao-thun-basic", "ATB-001-WHT-M", True),
    "ao-thun-basic-secondary": ("ao-thun-basic", "ATB-001-BLK-M", False),
    "quan-jean-relaxed": ("quan-jean-relaxed", "QJR-001-BLU-30", True),
    "ao-khoac-bomber": ("ao-khoac-bomber", "AKB-001-BLK-M", True),
    "dam-midi-linen": ("dam-midi-linen", "DML-001-SND-M", True),
    "ao-khoac-denim-lightwash": ("ao-khoac-denim-lightwash", "ADL-001-BLU-M", True),
    "ao-knit-tank-ivory": ("ao-knit-tank-ivory", "AKT-001-IVR-M", True),
    "tui-mini-crescent": ("tui-mini-crescent", "TMC-001-BLK-OS", True),
    "sneaker-cloud-runner": ("sneaker-cloud-runner", "SCR-001-GRY-40", True),
}


def load(path: Path) -> list[dict]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, list):
        raise ValueError(f"Manifest must be a JSON array: {path}")
    return value


def check_file(url: str) -> None:
    if not url.startswith("/assets/products/"):
        raise ValueError(f"Not a local product asset URL: {url}")
    target = PUBLIC / url.lstrip("/")
    if not target.is_file():
        raise FileNotFoundError(target)


def quote(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def target_group(number: int, name: str) -> tuple[str, tuple[str, ...]]:
    if 1 <= number <= 10 or number == 96:
        return "Tshirts", ("Tshirts",)
    if 21 <= number <= 28:
        return "Polo", ("Tshirts",)
    if 11 <= number <= 20:
        return "Shirts", ("Shirts",)
    if 29 <= number <= 34:
        return "Sweatshirts", ("Sweatshirts",)
    if 35 <= number <= 40:
        return "Sweaters", ("Sweaters",)
    if 41 <= number <= 44 or number == 49:
        return "Jeans", ("Jeans",)
    if number in {45, 46, 48, 50, 57, 58, 59, 60, 61, 62}:
        return "Trousers", ("Trousers",)
    if number in {47, 87}:
        return "Track Pants", ("Track Pants",)
    if number == 86:
        return "Sports Tshirt", ("Tshirts",)
    if number == 89:
        return "Sports Top", ("Tops",)
    if number == 90:
        return "Sports Set", ("Track Pants", "Shorts", "Tshirts")
    if number == 91:
        return "Running Jacket", ("Jackets",)
    if 51 <= number <= 56 or number == 97:
        return "Shorts", ("Shorts",)
    if 63 <= number <= 69:
        return "Skirts", ("Skirts",)
    if 70 <= number <= 77:
        return "Dresses", ("Dresses",)
    if 78 <= number <= 85:
        return "Jackets/Blazers", ("Jackets", "Blazers")
    if number == 88:
        return "Leggings", ("Leggings",)
    if number in {92, 93, 94, 95}:
        return "Homewear", ("Track Pants", "Shorts", "Tshirts")
    if 98 <= number <= 103:
        return "Footwear", ("Casual Shoes", "Sports Shoes", "Flip Flops", "Sandals", "Formal Shoes")
    if 104 <= number <= 109:
        return "Bags", ("Handbags", "Backpacks", "Clutches")
    if 110 <= number <= 115:
        return "Accessories", ("Belts", "Caps", "Wallets", "Socks", "Scarves")
    raise ValueError(f"No source article group specified for product number {number}: {name}")


def color_matches(target: str, source: str) -> bool:
    t, s = target.strip().lower(), source.strip().lower()
    if t == "black":
        return s == "black"
    if t == "white":
        return s in {"white", "off white"}
    if t in {"navy", "navy blue", "blue"}:
        return s in {"blue", "navy blue"}
    aliases = {"grey": {"grey", "grey melange", "charcoal"}, "gray": {"grey", "grey melange", "charcoal"}}
    return s in aliases.get(t, {t})


def generate_extended(manifest: list[dict], targets: list[dict]) -> tuple[list[dict], dict]:
    pool_by_type: dict[str, list[dict]] = defaultdict(list)
    for row in manifest:
        pool_by_type[row.get("sourceArticleType", "")].append(row)
    for rows in pool_by_type.values():
        rows.sort(key=lambda r: (int(r["datasetId"]), r["image"]))

    plans = []
    used_by_group: dict[str, set[str]] = defaultdict(set)
    reuse_counts: Counter[str] = Counter()
    for product in sorted(targets, key=lambda p: p["slug"]):
        match = re.search(r"-(\d{3})$", product["slug"])
        if not match:
            raise ValueError(f"Target product slug has no 3-digit suffix: {product['slug']}")
        number = int(match.group(1))
        label, article_types = target_group(number, product["name"])
        group = label
        variants = product["variants"]
        if not variants:
            raise ValueError(f"No variants found for {product['slug']}")
        candidates = [r for article in article_types for r in pool_by_type[article]]
        if label == "Polo":
            candidates = [r for r in candidates if "polo" in r["name"].lower()]
        if label == "Homewear":
            preferred = [r for r in candidates if r.get("sourceUsage", "").lower() in {"casual", "smart casual"}]
            if preferred:
                candidates = preferred
        if number in {86, 90}:
            preferred = [r for r in candidates if r.get("sourceUsage", "").lower() == "sports"]
            if preferred:
                candidates = preferred
        if not candidates:
            raise ValueError(f"No category-safe candidates for {product['slug']} ({label})")
        plans.append({"product": product, "number": number, "label": label, "group": group,
                      "candidates": candidates, "primary_variant": variants[0], "chosen": []})

    # Reserve distinct candidates for every product primary before considering secondary images.
    for plan in plans:
        candidates = plan["candidates"]
        color = plan["primary_variant"]["color"]
        colored = [r for r in candidates if color_matches(color, r.get("sourceColor", ""))]
        ordered = colored + [r for r in candidates if r not in colored]
        group_used = used_by_group[plan["group"]]
        selected = next((r for r in ordered if r["image"] not in group_used), None)
        reused = selected is None
        if reused:
            selected = ordered[0]
            reuse_counts[plan["group"]] += 1
        else:
            group_used.add(selected["image"])
        target_color = plan["primary_variant"]["color"]
        color_match = color_matches(target_color, selected.get("sourceColor", ""))
        plan["chosen"].append((plan["primary_variant"], selected, reused, color_match))

    # Once all primaries are protected, attach remaining unique same-color assets as secondary photos.
    for plan in plans:
        product_used = {plan["chosen"][0][1]["image"]}
        group_used = used_by_group[plan["group"]]
        for variant in plan["product"]["variants"][1:]:
            color_rows = [r for r in plan["candidates"] if color_matches(variant["color"], r.get("sourceColor", ""))]
            selected = next((r for r in color_rows if r["image"] not in group_used and r["image"] not in product_used), None)
            if selected:
                plan["chosen"].append((variant, selected, False, True))
                product_used.add(selected["image"])
                group_used.add(selected["image"])

    result = []
    for plan in plans:
        for order, (variant, row, was_reused, color_match) in enumerate(plan["chosen"], 1):
            product = plan["product"]
            result.append({
                "productSlug": product["slug"], "variantSku": variant["sku"],
                "image": row["image"], "sourceName": row["name"],
                "sourceArticleType": row["sourceArticleType"], "sourceColor": row["sourceColor"],
                "sourceUsage": row.get("sourceUsage"), "targetGroup": plan["label"],
                "isPrimary": order == 1, "displayOrder": order,
                "targetVariantColor": variant["color"], "colorMatch": color_match,
                "controlledReuse": was_reused,
                "altText": f"{product['name']} - {row['name']} ({row['sourceArticleType']}, {row['sourceColor']})",
            })
    return result, dict(reuse_counts)


def sql_fragment(base_entries: list[dict], extended_entries: list[dict]) -> str:
    values = []
    for row in extended_entries:
        values.append("(" + ", ".join([
            quote(row["productSlug"]), quote(row["variantSku"]), quote(row["image"]),
            quote(row["altText"]), "TRUE" if row["isPrimary"] else "FALSE", str(row["displayOrder"]),
        ]) + ")")
    slugs = sorted({row["productSlug"] for row in extended_entries})
    slug_values = ", ".join(quote(slug) for slug in slugs)
    return (
        "-- Generated by scripts/build_seed_images.py from seed-image-map.json.\n"
        "-- PostgreSQL statements; only product_images rows are changed.\n"
        "BEGIN TRANSACTION;\nDELETE FROM product_images\n"
        "WHERE product_id IN (SELECT id FROM products WHERE slug IN (" + slug_values + "));\n"
        "INSERT INTO product_images (product_id, variant_id, image_url, alt_text, is_primary, display_order)\n"
        "SELECT p.id, v.id, m.image_url, m.alt_text, m.is_primary, m.display_order\n"
        "FROM (VALUES\n" + ",\n".join("    " + v for v in values) + "\n) "
        "AS m(product_slug, variant_sku, image_url, alt_text, is_primary, display_order)\n"
        "JOIN products p ON p.slug=m.product_slug\n"
        "JOIN product_variants v ON v.product_id=p.id AND v.sku=m.variant_sku;\nCOMMIT TRANSACTION;\n"
    )


def main() -> None:
    base_rows, manifest = load(BASE_MANIFEST), load(SAMPLE_MANIFEST)
    base_by_key = {r.get("seedKey"): r for r in base_rows}
    if set(BASE) - set(base_by_key):
        raise ValueError(f"Missing base seed keys: {sorted(set(BASE) - set(base_by_key))}")
    for row in base_rows:
        check_file(row["localImage"])
    for row in manifest:
        check_file(row["image"])
    # Every manifest entry must point to a unique file and dataset ID.
    for key in ("datasetId", "image"):
        values = [r[key] for r in manifest]
        if len(values) != len(set(values)):
            raise ValueError(f"Duplicate {key} in Myntra manifest")

    base_entries = []
    for key, (slug, sku, primary) in BASE.items():
        row = base_by_key[key]
        base_entries.append({
            "productSlug": slug, "variantSku": sku, "image": row["localImage"],
            "sourceName": row["sourceName"], "sourceArticleType": row["articleType"],
            "sourceColor": row["color"], "isPrimary": primary,
            "displayOrder": 1 if primary else 2,
            "altText": f"{row['sourceName']} – {row['articleType']} ({row['color']})",
        })

    # Read only live target slug/variant rows to build a reviewed mapping. SQL export is a
    # separate image-only fragment and does not alter product prices, stock, or catalog text.
    targets = load(TARGET_SNAPSHOT)
    extended_entries, reuse = generate_extended(manifest, targets)
    for row in base_entries + extended_entries:
        check_file(row["image"])
    map_data = {"baseProducts": base_entries, "extendedProducts": extended_entries,
                "controlledReuseCounts": reuse}
    MAP_JSON.write_text(json.dumps(map_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    generated_map = json.loads(MAP_JSON.read_text(encoding="utf-8"))
    SQL_OUT.write_text(sql_fragment([], generated_map["extendedProducts"]), encoding="utf-8")
    with AUDIT_CSV.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["productSlug", "image", "sourceArticleType", "sourceColor", "targetGroup", "variantSku", "isPrimary", "displayOrder", "sourceName"])
        writer.writeheader()
        writer.writerows({k: r.get(k) for k in writer.fieldnames} for r in base_entries + extended_entries)
    update_seed_base_paths(base_rows)
    print(f"Verified files: seed-catalog {len(base_rows)}/{len(base_rows)}, Myntra {len(manifest)}/{len(manifest)}")
    print(f"Generated base mappings: {len(base_entries)}; extended product image rows: {len(extended_entries)}")
    print(f"Controlled image reuse counts by target group: {json.dumps(reuse, ensure_ascii=False)}")
    print(f"Wrote {MAP_JSON.relative_to(ROOT)}, {SQL_OUT.relative_to(ROOT)}, {AUDIT_CSV.relative_to(ROOT)}")


def update_seed_base_paths(base_rows: list[dict]) -> None:
    text = SEED_SQL.read_text(encoding="utf-8")
    by_key = {r["seedKey"]: r for r in base_rows}
    for key in BASE:
        check_file(by_key[key]["localImage"])

    start_marker = "-- Seed product photos are local files under frontend/public/assets/products; replace stale images on every seed run."
    end_marker = "INSERT INTO size_guides"
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    rows = []
    for key, (slug, sku, primary) in BASE.items():
        row = by_key[key]
        rows.append("(" + ", ".join([
            quote(slug), quote(sku), quote(row["localImage"]),
            quote(f"{row['sourceName']} - {row['articleType']} ({row['color']})"),
            "TRUE" if primary else "FALSE", "1" if primary else "2",
        ]) + ")")
    managed_slugs = sorted({value[0] for key, value in BASE.items() if key != "dam-midi-linen"})
    section = start_marker + "\nDELETE FROM product_images i USING products p\nWHERE i.product_id=p.id AND p.slug IN (" + ",".join(quote(slug) for slug in managed_slugs) + ");\n"
    section += "INSERT INTO product_images (product_id,variant_id,image_url,alt_text,is_primary,display_order)\n"
    section += "SELECT p.id,v.id,m.image_url,m.alt_text,m.is_primary,m.display_order\nFROM (VALUES\n    " + ",\n    ".join(rows) + "\n) AS m(product_slug,variant_sku,image_url,alt_text,is_primary,display_order)\n"
    section += "JOIN products p ON p.slug=m.product_slug JOIN product_variants v ON v.product_id=p.id AND v.sku=m.variant_sku\nWHERE (p.name='Arrow Woman Solid Cream Dress' AND NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id=p.id AND i.variant_id=v.id AND i.image_url=m.image_url)) OR p.slug<>'dam-midi-linen';\n\n"
    text = text[:start] + section + text[end:]
    # Review photo uses the white tee photo from the same local seed catalog.
    review_path = by_key["ao-thun-basic-primary"]["localImage"]
    text = re.sub(r"(SELECT r.id, )'[^']+'", lambda m: m.group(1) + quote(review_path), text, count=1)
    text = re.sub(r"(AND prior.image_url = )'[^']+'", lambda m: m.group(1) + quote(review_path), text, count=1)
    text = re.sub(r"(SET image_url=)'[^']+'", lambda m: m.group(1) + quote(review_path), text, count=1)
    # Make generated Myntra sample alt text legible and derived from manifest columns.
    sample_image_select = "SELECT p.id,(SELECT v.id FROM product_variants v WHERE v.product_id=p.id ORDER BY v.id LIMIT 1),s.image_url,(U&'\\1EA2nh s\\1EA3n ph\\1EA9m '||s.product_name||' ('||COALESCE(s.article_type,U&'th\\1EDD' || 'i trang')||', '||U&'m\\00E0u'||' '||COALESCE(NULLIF(s.color,'Assorted'),U&'ch\\01B0a r\\1ECF')||')'),TRUE,1"
    text = re.sub(r"SELECT p.id,\(SELECT v.id FROM product_variants v WHERE v.product_id=p.id ORDER BY v.id LIMIT 1\),s.image_url,.*?,TRUE,1",
                  lambda _: sample_image_select, text)
    image_marker = "-- BEGIN GENERATED 115-PRODUCT IMAGE MAP"
    image_end = "-- END GENERATED 115-PRODUCT IMAGE MAP"
    generated = SQL_OUT.read_text(encoding="utf-8").strip()
    if image_marker in text:
        before = text[:text.index(image_marker)].rstrip()
        after_start = text.index(image_end, text.index(image_marker)) + len(image_end)
        after = text[after_start:].lstrip("\r\n")
        text = before + "\n" + image_marker + "\n" + generated + "\n" + image_end + "\n" + after
    else:
        text = text.rstrip() + "\n\n" + image_marker + "\n" + generated + "\n" + image_end + "\n"
    unexpected_external = re.findall(r"https?://[^'\s]+", text, re.I)
    allowed_hity_image = "https://product.hstatic.net/1000154340/product/vai_lanh_do_dam_co_tim_kieu_vintage_co_dien_thoi_trang_thiet_ke_hity_2_dd759789d7d4499c91a79600b78dfaac_master.jpg"
    if any(url != allowed_hity_image for url in unexpected_external):
        raise ValueError("External URL remains in seed.sql")
    SEED_SQL.write_text(text, encoding="utf-8", newline="")


if __name__ == "__main__":
    main()
