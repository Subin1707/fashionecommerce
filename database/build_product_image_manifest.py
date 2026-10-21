"""Export the current catalog and audit local product image assets.

The manifest distinguishes database products from verified local image files.
It never downloads images or marks remote URLs as verified automatically.
"""

from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
from urllib.parse import urlparse

import psycopg


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "frontend" / "public" / "assets"
OUTPUT = ASSET_ROOT / "products-manifest.json"
CATEGORY_REPORT = ASSET_ROOT / "PRODUCT_IMAGE_CATEGORY_INVENTORY.md"
DB_CONFIG = {
    "host": os.getenv("FASHION_DB_HOST", "localhost"),
    "port": int(os.getenv("FASHION_DB_PORT", "5432")),
    "dbname": os.getenv("FASHION_DB_NAME", "fashion"),
    "user": os.getenv("FASHION_DB_USER", "fashion"),
    "password": os.getenv("FASHION_DB_PASSWORD", "fashion"),
}


def local_path(image_url: str) -> Path | None:
    parsed = urlparse(image_url)
    if parsed.scheme or not parsed.path.startswith("/assets/"):
        return None
    return ROOT / "frontend" / "public" / parsed.path.lstrip("/")


def inspect_file(path: Path) -> dict:
    result = {
        "exists": path.is_file(),
        "readable": False,
        "format": None,
        "sha256": None,
        "phash": None,
        "bytes": None,
        "duplicateStatus": "not_checked",
    }
    if not path.is_file():
        return result

    try:
        data = path.read_bytes()
        result["bytes"] = len(data)
        result["sha256"] = hashlib.sha256(data).hexdigest()
        try:
            from PIL import Image

            with Image.open(path) as image:
                image.verify()
                result["format"] = image.format
            with Image.open(path) as image:
                grayscale = image.convert("L").resize((9, 8))
                pixels = list(grayscale.tobytes())
                result["phash"] = "".join(
                    "1" if pixels[row * 9 + column] > pixels[row * 9 + column + 1] else "0"
                    for row in range(8)
                    for column in range(8)
                )
            result["readable"] = True
            result["duplicateStatus"] = "sha256_and_dhash_checked"
            try:
                import imagehash

                with Image.open(path) as image:
                    result["phash"] = str(imagehash.phash(image))
                result["duplicateStatus"] = "sha256_and_phash_checked"
            except ImportError:
                pass
        except ImportError:
            result["duplicateStatus"] = "sha256_checked; install_pillow_for_decode_and_phash"
        except Exception as error:  # Pillow rejects unreadable or invalid images.
            result["decodeError"] = str(error)
    except OSError as error:
        result["readError"] = str(error)
    return result


def readable_category_name(slug: str, name: str) -> str:
    # Several existing seed rows contain mojibake Vietnamese names. Preserve the
    # database value in products-manifest.json, but use the stable slug in reports.
    mojibake_markers = ("Ã", "Ä", "Ă", "Â", "á»", "Æ", "Ä‘", "áº")
    if any(marker in name for marker in mojibake_markers):
        return slug.replace("-", " ").title()
    return name


def main() -> None:
    batch_manifests = sorted(ASSET_ROOT.glob("vietnamese-batch-*-manifest.json"))
    downloaded_assets = [
        item
        for batch_manifest in batch_manifests
        for item in json.loads(batch_manifest.read_text(encoding="utf-8"))
    ]
    sourced_images = {item["localImage"]: item for item in downloaded_assets}

    query = """
        SELECT p.slug, p.name, c.slug, c.name, p.status,
               COALESCE(p.sale_price, p.base_price),
               COALESCE(images.urls, '[]'::jsonb)
        FROM products p
        JOIN categories c ON c.id = p.category_id
        LEFT JOIN LATERAL (
            SELECT jsonb_agg(jsonb_build_object(
                'url', image.image_url,
                'alt', image.alt_text,
                'primary', image.is_primary,
                'displayOrder', image.display_order,
                'variantId', image.variant_id
            ) ORDER BY image.is_primary DESC, image.display_order, image.id) AS urls
            FROM product_images image
            WHERE image.product_id = p.id
        ) images ON TRUE
        ORDER BY c.slug, p.slug
    """
    with psycopg.connect(**DB_CONFIG) as connection:
        categories = connection.execute(
            """
            SELECT c.slug, c.name, c.is_active,
                   NOT EXISTS (
                       SELECT 1 FROM categories child
                       WHERE child.parent_id = c.id AND child.is_active = TRUE
                   ) AS is_active_leaf
            FROM categories c ORDER BY c.slug
            """
        ).fetchall()
        rows = connection.execute(query).fetchall()

    products = []
    category_totals: dict[str, dict] = {
        slug: {"name": name, "active": active, "activeLeaf": active_leaf,
               "products": 0, "withLocalImage": 0, "missingLocalImage": 0}
        for slug, name, active, active_leaf in categories
    }
    sha_owners: dict[str, list[str]] = {}
    perceptual_hashes: list[tuple[str, str]] = []
    audited_assets = []
    rejected_assets = []
    for slug, name, category_slug, category_name, status, price, images in rows:
        category = category_totals.setdefault(
            category_slug,
            {"name": category_name, "active": True, "activeLeaf": True,
             "products": 0, "withLocalImage": 0, "missingLocalImage": 0},
        )
        if status == "ACTIVE":
            category["products"] += 1

        checked_images = []
        for image in images:
            path = local_path(image["url"])
            audit = inspect_file(path) if path else {
                "exists": None,
                "readable": None,
                "format": None,
                "sha256": None,
                "phash": None,
                "bytes": None,
                "duplicateStatus": "remote_url_not_downloaded_or_verified",
            }
            provenance = sourced_images.get(image["url"], {})
            checked_images.append({
                **image,
                "localFile": str(path.relative_to(ROOT)).replace("\\", "/") if path else None,
                "sourcePage": provenance.get("sourcePage"),
                "creator": provenance.get("creator"),
                "license": provenance.get("license", "not_verified"),
                "licenseUrl": provenance.get("licenseUrl"),
                "modifications": provenance.get("modifications"),
                "audit": audit,
            })

        has_valid_local = any(image["audit"]["readable"] for image in checked_images)
        if status == "ACTIVE":
            category["withLocalImage" if has_valid_local else "missingLocalImage"] += 1
        products.append({
            "id": slug,
            "name": name,
            "category": {"id": category_slug, "name": category_name},
            "status": status,
            "displayPrice": str(price),
            "images": checked_images,
            "catalogVerification": "database_record_only",
        })

    for asset in downloaded_assets:
        relative_path = asset.get("localImage", "").removeprefix("/assets/")
        path = ASSET_ROOT / relative_path
        audit = inspect_file(path)
        asset_id = asset.get("assetId", asset.get("productId"))
        if audit["sha256"]:
            sha_owners.setdefault(audit["sha256"], []).append(asset_id)
        if audit["phash"]:
            perceptual_hashes.append((asset_id, audit["phash"]))
        audited_assets.append({**asset, "audit": audit})

    rejected_root = ASSET_ROOT / "_review" / "rejected"
    rejected_manifest = ASSET_ROOT / "vietnamese-rejected-manifest.json"
    rejected_metadata = json.loads(rejected_manifest.read_text(encoding="utf-8")) if rejected_manifest.is_file() else []
    rejected_by_id = {item["assetId"]: item for item in rejected_metadata}
    if rejected_root.is_dir():
        for folder in rejected_root.iterdir():
            image = folder / "main.webp"
            if not image.is_file():
                continue
            metadata = rejected_by_id.get(folder.name, {})
            rejected_assets.append({
                **metadata,
                "assetId": folder.name,
                "localImage": "/assets/" + image.relative_to(ASSET_ROOT).as_posix(),
                "status": "rejected_after_visual_review",
                "audit": inspect_file(image),
            })

    use_p_hash = any(
        item.get("audit", {}).get("duplicateStatus") == "sha256_and_phash_checked"
        for item in audited_assets
    )

    exact_duplicates = {digest: owners for digest, owners in sha_owners.items() if len(owners) > 1}
    near_duplicates = []
    for index, (left_slug, left_hash) in enumerate(perceptual_hashes):
        for right_slug, right_hash in perceptual_hashes[index + 1 :]:
            distance = sum(a != b for a, b in zip(left_hash, right_hash))
            if left_slug != right_slug and distance <= 5:
                near_duplicates.append({"products": [left_slug, right_slug], "hashDistance": distance})
    report = {
        "generatedBy": "database/build_product_image_manifest.py",
        "scope": "Current database catalog; remote image contents and license are not asserted as verified.",
        "summary": {
            "activeProducts": sum(1 for product in products if product["status"] == "ACTIVE"),
            "activeLeafCategories": sum(1 for item in category_totals.values() if item["active"] and item["activeLeaf"]),
            "downloadedVerifiedImages": sum(1 for item in audited_assets if item["audit"]["readable"]),
            "exactDuplicateLocalFiles": len(exact_duplicates),
            "nearDuplicateImages": len(near_duplicates),
            "perceptualHashMethod": "pHash" if use_p_hash else "dHash",
            "categoriesUnderTenActiveProducts": [
                {"slug": slug, **counts}
                for slug, counts in sorted(category_totals.items())
                if counts["active"] and counts["activeLeaf"] and counts["products"] < 10
            ],
        },
        "exactDuplicateGroups": exact_duplicates,
        "nearDuplicateGroups": near_duplicates,
        "downloadedAssets": audited_assets,
        "rejectedAssets": rejected_assets,
        "categories": category_totals,
        "products": products,
    }
    ASSET_ROOT.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    category_lines = [
        "# Kiểm kê danh mục sản phẩm và ảnh đã liên kết",
        "",
        "Báo cáo được tạo từ database hiện tại và chỉ tính ảnh local hợp lệ đã liên kết trong `product_images`. Ảnh tham khảo chưa gắn SKU không được tính vào cột ảnh hợp lệ.",
        "",
        f"- Tổng danh mục: **{len(category_totals)}**; danh mục lá active: **{report['summary']['activeLeafCategories']}**; sản phẩm ACTIVE: **{report['summary']['activeProducts']}**.",
        f"- Danh mục lá active có dưới 10 sản phẩm ACTIVE: **{len(report['summary']['categoriesUnderTenActiveProducts'])}**.",
        f"- Ảnh tham khảo đã kiểm tra giấy phép trong các batch: **{len(audited_assets)}**; không tính là sản phẩm mới.",
        f"- Ảnh local gắn với sản phẩm ACTIVE: **{sum(item['withLocalImage'] for item in category_totals.values())}**; sản phẩm ACTIVE còn thiếu ảnh local hợp lệ: **{sum(item['missingLocalImage'] for item in category_totals.values())}**.",
        "",
        "| Slug danh mục | Tên | Active | Danh mục lá | Sản phẩm ACTIVE | Có ảnh local hợp lệ | Thiếu ảnh local | Mốc 10 |",
        "|---|---|---|---|---:|---:|---:|---|",
    ]
    for slug, counts in sorted(category_totals.items()):
        state = "Đạt mốc 10" if counts["products"] >= 10 else "Thiếu sản phẩm"
        if not counts["active"]:
            state = "Không active"
        elif not counts["activeLeaf"]:
            state = "Danh mục cha"
        category_label = readable_category_name(slug, counts["name"])
        category_lines.append(
            f"| `{slug}` | {category_label} | {'Có' if counts['active'] else 'Không'} | {'Có' if counts['activeLeaf'] else 'Không'} | {counts['products']} | {counts['withLocalImage']} | {counts['missingLocalImage']} | {state} |"
        )
    category_lines.extend([
        "",
        "## Phạm vi và giới hạn",
        "",
        "Đây là kiểm kê hiện trạng, không tự tạo sản phẩm, giá bán, thương hiệu hoặc biến thể. Danh mục lá active là danh mục active không có danh mục con active. Ảnh tham khảo chỉ được gắn vào sản phẩm khi có căn cứ phù hợp chính xác. Ảnh CC BY/CC BY-SA cần giữ attribution và tuân thủ điều kiện license khi phân phối.",
        "",
    ])
    CATEGORY_REPORT.write_text("\n".join(category_lines), encoding="utf-8")
    print(f"Manifest: {OUTPUT.relative_to(ROOT)}")
    print(f"Category inventory: {CATEGORY_REPORT.relative_to(ROOT)}")
    print(f"Active products: {report['summary']['activeProducts']}; categories: {report['summary']['categories']}")
    print(f"Categories below 10 active products: {len(report['summary']['categoriesUnderTenActiveProducts'])}")
    print(f"Exact duplicate local assets: {report['summary']['exactDuplicateLocalFiles']}")


if __name__ == "__main__":
    main()

