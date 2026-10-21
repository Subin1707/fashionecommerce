"""Fetch verified, licensed Commons reference assets for everyday fashion categories."""

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
    ("dress-colourful", "Women's dress reference", "womenswear/dresses", "File:Women's dress.jpg"),
    ("dress-women", "Women dress reference", "womenswear/dresses", "File:Women dress.jpg"),
    ("dress-little-black", "Little black dress reference", "womenswear/dresses", "File:Little black dress outfit.jpg"),
    ("dress-modest", "Modest fashion dress reference", "womenswear/dresses", "File:Modest fashion dress.jpg"),
    ("dress-museum-white", "White muslin dress reference", "womenswear/dresses", "File:Dress, women's (AM 1995.8.14-1).jpg"),
    ("jacket-utility", "Utility shirt jacket reference", "menswear/jackets", "File:Utility Shirt Jacket.jpg"),
    ("jacket-tailoring", "Tailored jacket fitting reference", "menswear/jackets", "File:TailoringFirstFitFront02.jpg"),
    ("tee-round-neck", "Round neck T-shirt reference", "menswear/tops", "File:T-Shirt.jpg"),
    ("tee-v-neck", "V-neck T-shirt reference", "menswear/tops", "File:T-shirt v neck.jpg"),
    ("tee-stock", "Cotton T-shirt reference", "menswear/tops", "File:Mans t-shirt stock original goods.jpg"),
    ("sandals-walking", "Walking sandals reference", "shoes/sandals", "File:Sandals walking.jpg"),
    ("sandals-leather", "Leather sandals reference", "shoes/sandals", "File:Leather Sandals.jpg"),
    ("sandals-strappy", "Strappy sandals reference", "shoes/sandals", "File:Image of strappy sandals.png"),
    ("sandals-handcrafted", "Handcrafted leather sandals reference", "shoes/sandals", "File:Handcrafted Leather Sandals.jpg"),
    ("sandals-socks", "Sandals reference", "shoes/sandals", "File:Sandals and Socks.jpg"),
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
    target = ASSET_ROOT / "vietnamese-batch-05-manifest.json"
    target.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Downloaded {len(results)}/{len(ITEMS)}; manifest {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
