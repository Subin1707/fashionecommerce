"""Download a small curated Commons batch, preserving attribution and licenses."""

from __future__ import annotations

import hashlib
import json
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "frontend" / "public" / "assets"
ITEMS = [
    ("sneaker-classic", "Sneaker tham khảo", "sneaker-giay-the-thao/sneaker-giay-the-thao", "File:Sneaker.jpg"),
    ("sneaker-velcro", "Sneaker quai dán tham khảo", "sneaker-giay-the-thao/sneaker-giay-the-thao", "File:Sneakers velcro.jpg"),
    ("sneaker-adidas-stan-smith", "Sneaker Stan Smith tham khảo", "sneaker-giay-the-thao/sneaker-giay-the-thao", "File:Adidas Stan Smiths sneaker.jpg"),
    ("ballet-flat-table", "Giày búp bê tham khảo", "sneaker-giay-the-thao/giay-cao-got-bup-be", "File:Table full of ballerina shoes.jpg"),
    ("handbag-green", "Túi xách xanh tham khảo", "phu-kien-thoi-trang/tui-xach-balo", "File:Handbag image.jpg"),
    ("handbag-black", "Túi xách đen tham khảo", "phu-kien-thoi-trang/tui-xach-balo", "File:Black handbag.jpg"),
    ("handbag-womens", "Túi nữ tham khảo", "phu-kien-thoi-trang/tui-xach-balo", "File:Women's handbag.jpg"),
    ("handbag-kate-spade", "Túi xách tham khảo", "phu-kien-thoi-trang/tui-xach-balo", "File:Kate Spade handbag.jpg"),
    ("wallet-vintage-1", "Ví da tham khảo", "phu-kien-thoi-trang/vi-that-lung", "File:Wallet (AM 1969.3-3).jpg"),
    ("wallet-vintage-2", "Ví da tham khảo", "phu-kien-thoi-trang/vi-that-lung", "File:Wallet (AM 1969.3-4).jpg"),
    ("wallet-vintage-3", "Ví da tham khảo", "phu-kien-thoi-trang/vi-that-lung", "File:Wallet, leather (AM 2015.34.68-4).jpg"),
    ("necklace-pendant", "Dây chuyền mặt đá tham khảo", "phu-kien-thoi-trang/dong-ho-trang-suc", "File:Necklace and pendant.jpg"),
    ("necklace-fashion", "Dây chuyền thời trang tham khảo", "phu-kien-thoi-trang/dong-ho-trang-suc", "File:Fashion Jewelry Necklace.jpg"),
    ("necklace-handmade", "Dây chuyền thủ công tham khảo", "phu-kien-thoi-trang/dong-ho-trang-suc", "File:Finishednecklace.jpg"),
    ("watch-black", "Đồng hồ đeo tay tham khảo", "phu-kien-thoi-trang/dong-ho-trang-suc", "File:Wrist watch black.jpg"),
    ("watch-sonata", "Đồng hồ đeo tay tham khảo", "phu-kien-thoi-trang/dong-ho-trang-suc", "File:Sonata wrist watch.jpg"),
]
UA = "FashionShopImageCatalog/1.0 (local educational catalog; contact admin@fashion.local)"


def commons_info(title: str) -> dict:
    query = urllib.parse.urlencode({
        "action": "query", "titles": title, "prop": "imageinfo",
        "iiprop": "url|extmetadata", "iiurlwidth": "720", "format": "json",
    })
    req = urllib.request.Request(f"https://commons.wikimedia.org/w/api.php?{query}", headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as response:
        page = next(iter(json.load(response)["query"]["pages"].values()))
    if "imageinfo" not in page:
        raise RuntimeError(f"No file on Commons: {title}")
    return page["imageinfo"][0]


def clean(meta: dict, key: str) -> str:
    return re.sub(r"<[^>]*>", " ", meta.get(key, {}).get("value", "")).strip()


def main() -> None:
    results = []
    for asset_id, label, category, title in ITEMS:
        try:
            info = commons_info(title)
            meta = info.get("extmetadata", {})
            license_name = clean(meta, "LicenseShortName")
            license_url = clean(meta, "LicenseUrl")
            if not license_name or not license_url:
                raise RuntimeError(f"Missing verifiable license metadata for {title}")
            folder = ASSET_ROOT / "products" / category / asset_id
            folder.mkdir(parents=True, exist_ok=True)
            source_path, output_path = folder / "source.jpg", folder / "main.webp"
            req = urllib.request.Request(info.get("thumburl", info["url"]), headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=40) as response:
                source_path.write_bytes(response.read())
            with Image.open(source_path) as image:
                image.verify()
            with Image.open(source_path) as image:
                image = image.convert("RGB")
                image.thumbnail((720, 960), Image.Resampling.LANCZOS)
                image.save(output_path, "WEBP", quality=82, method=6)
                width, height = image.size
            data = output_path.read_bytes()
            results.append({
                "assetId": asset_id, "displayName": label, "category": category,
                "commonsTitle": title,
                "sourcePage": "https://commons.wikimedia.org/wiki/" + urllib.parse.quote(title.replace(" ", "_"), safe=":_()'.,-"),
                "creator": clean(meta, "Artist") or clean(meta, "Credit") or "See Commons source page",
                "license": license_name, "licenseUrl": license_url,
                "modifications": "Downloaded Commons thumbnail; resized within 720x960 and converted to WebP; source thumbnail retained as source.jpg.",
                "localImage": "/assets/" + output_path.relative_to(ASSET_ROOT).as_posix(),
                "localSource": "/assets/" + source_path.relative_to(ASSET_ROOT).as_posix(),
                "format": "WEBP", "width": width, "height": height, "bytes": len(data),
                "sha256": hashlib.sha256(data).hexdigest(), "verified": True,
            })
            print(f"OK {title}: {license_name}, {width}x{height}, {len(data)} bytes")
        except Exception as exc:
            print(f"SKIP {title}: {exc}")
        time.sleep(0.4)
    target = ASSET_ROOT / "vietnamese-batch-03-manifest.json"
    target.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Downloaded {len(results)}/{len(ITEMS)}; manifest {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
