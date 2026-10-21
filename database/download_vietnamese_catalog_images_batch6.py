"""Fetch verified, licensed Commons reference assets for denim, knitwear, skirts and coats."""

from __future__ import annotations

import hashlib
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "frontend" / "public" / "assets"
ITEMS = [
    ("jeans-blue", "Blue jeans reference", "womenswear/jeans", "File:02017 blue jeans.jpg"),
    ("skirt-cream", "Cream full skirt reference", "womenswear/skirts", "File:Cream full skirt.jpg"),
    ("skirt-wrap", "Wrap skirt reference", "womenswear/skirts", "File:Wrapskirt.jpg"),
    ("skirt-full", "Full skirt reference", "womenswear/skirts", "File:Woman wearing full skirt.jpg"),
    ("skirt-tight", "Tight skirt reference", "womenswear/skirts", "File:Tight skirt.jpg"),
    ("knit-grey", "Grey knitted sweater reference", "womenswear/knitwear", "File:Grey knitted sweater.jpg"),
    ("knit-cashmere", "Cashmere sweater reference", "womenswear/knitwear", "File:Cashmere Sweater.jpg"),
    ("knit-handmade", "Hand knitted sweater reference", "womenswear/knitwear", "File:Hand knitted sweater.jpg"),
    ("knit-machine", "Knitted sweater reference", "womenswear/knitwear", "File:Knitted Sweater.jpg"),
    ("coat-woman-1", "Women's coat reference", "womenswear/coats", "File:Coat, woman's (AM 13365-3).jpg"),
    ("coat-woman-2", "Women's coat reference", "womenswear/coats", "File:Coat, woman's (AM 1996.207.2-4).jpg"),
    ("coat-jean-muir", "Tailored coat reference", "womenswear/coats", "File:Coat by Jean Muir.jpg"),
]
UA = "FashionShopImageCatalog/1.0 (local educational catalog; contact admin@fashion.local)"
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="backslashreplace")


def clean(meta: dict, key: str) -> str:
    return re.sub(r"<[^>]*>", " ", meta.get(key, {}).get("value", "")).strip()


def fetch_info(title: str) -> dict:
    query = urllib.parse.urlencode({"action": "query", "titles": title, "prop": "imageinfo", "iiprop": "url|extmetadata", "iiurlwidth": "720", "format": "json"})
    request = urllib.request.Request("https://commons.wikimedia.org/w/api.php?" + query, headers={"User-Agent": UA})
    with urllib.request.urlopen(request, timeout=30) as response:
        page = next(iter(json.load(response)["query"]["pages"].values()))
    if "imageinfo" not in page:
        raise RuntimeError("No imageinfo on Commons")
    return page["imageinfo"][0]


def main() -> None:
    results = []
    for asset_id, label, category, title in ITEMS:
        try:
            info = fetch_info(title)
            meta = info.get("extmetadata", {})
            license_name, license_url = clean(meta, "LicenseShortName"), clean(meta, "LicenseUrl")
            if not license_name or not license_url:
                raise RuntimeError("Missing verifiable license metadata")
            folder = ASSET_ROOT / "products" / category / asset_id
            folder.mkdir(parents=True, exist_ok=True)
            source_path, output_path = folder / "source.jpg", folder / "main.webp"
            request = urllib.request.Request(info.get("thumburl", info["url"]), headers={"User-Agent": UA})
            with urllib.request.urlopen(request, timeout=40) as response:
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
        except Exception as error:
            print(f"SKIP {title}: {error}")
        time.sleep(0.4)
    target = ASSET_ROOT / "vietnamese-batch-06-manifest.json"
    target.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Downloaded {len(results)}/{len(ITEMS)}; manifest {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
