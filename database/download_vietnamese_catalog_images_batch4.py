"""Download additional licensed Vietnamese dress reference images."""

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
    ("ao-dai-two-girls", "Two school girls in white ao dai", "File:Two girls in white aodai.jpg"),
    ("ao-dai-ngoc-bich-ngan", "Light green ao dai portrait", "File:NgocBichNganInLightGreenAoDai.jpg"),
    ("ao-dai-saigon-1960", "Ao dai in Saigon, 1960", "File:Áo dài Saigon 1960.jpg"),
    ("ao-dai-hoa-phuong", "Ao dai hoa phuong", "File:Áo dài hoa phượng - Phát Trương - 51231007393 (extend cropped).jpg"),
    ("ao-dai-hanoi-old-quarter", "Ao dai in Hanoi old quarter", "File:Young Woman in Ao Dai Dress - Old Quarter - Hanoi - Vietnam (48072077206).jpg"),
    ("ao-dai-tet-2014", "Ao dai at Tet event", "File:AO DAI TET 2014.01.04 - EP 10.jpg"),
    ("ao-dai-three-non-la", "Ao dai and non la", "File:ThreeNonLa.jpg"),
    ("ao-dai-festival-3", "Ao dai festival style", "File:Mot so kieu ao dai le hoi.JPG"),
    ("ao-ba-ba-vietnam-2011", "Ao ba ba in Vietnam", "File:Vietnam 2011.jpg"),
    ("ao-ba-ba-vietnam-2007", "Ao ba ba in Vietnam, 2007", "File:Vietnam 2007 252.jpg"),
    ("ao-ba-ba-portrait", "Ao ba ba in portrait", "File:Áo bà ba.png"),
    ("ethnic-hmong-costume", "Hmong traditional clothing in Vietnam", "File:Traditional Vietnamese dress.jpg"),
]
UA = "FashionShopImageCatalog/1.0 (local educational catalog; contact admin@fashion.local)"
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="backslashreplace")


def fetch_info(title: str) -> dict:
    query = urllib.parse.urlencode({"action": "query", "titles": title, "prop": "imageinfo", "iiprop": "url|extmetadata", "iiurlwidth": "720", "format": "json"})
    req = urllib.request.Request("https://commons.wikimedia.org/w/api.php?" + query, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as response:
        page = next(iter(json.load(response)["query"]["pages"].values()))
    if "imageinfo" not in page:
        raise RuntimeError("No Commons file")
    return page["imageinfo"][0]


def clean(meta: dict, key: str) -> str:
    return re.sub(r"<[^>]*>", " ", meta.get(key, {}).get("value", "")).strip()


def main() -> None:
    results = []
    for asset_id, label, title in ITEMS:
        try:
            info = fetch_info(title)
            meta = info.get("extmetadata", {})
            license_name, license_url = clean(meta, "LicenseShortName"), clean(meta, "LicenseUrl")
            if not license_name or not license_url:
                raise RuntimeError("Missing verifiable license metadata")
            directory = ASSET_ROOT / "products" / "vietnamese-traditional" / "ao-dai" / asset_id
            if asset_id.startswith("ao-ba-ba"):
                directory = ASSET_ROOT / "products" / "vietnamese-traditional" / "ao-ba-ba" / asset_id
            if asset_id.startswith("ethnic-"):
                directory = ASSET_ROOT / "products" / "vietnamese-traditional" / "ethnic-costumes" / asset_id
            directory.mkdir(parents=True, exist_ok=True)
            source_path, output_path = directory / "source.jpg", directory / "main.webp"
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
                "assetId": asset_id, "displayName": label,
                "category": str(directory.parent.relative_to(ASSET_ROOT / "products")).replace("\\", "/"),
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
    target = ASSET_ROOT / "vietnamese-batch-04-manifest.json"
    target.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Downloaded {len(results)}/{len(ITEMS)}; manifest {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
