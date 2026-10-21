"""Download and optimize three individually sourced Vietnamese garment photos."""

from __future__ import annotations

import hashlib
import json
import urllib.request
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "frontend" / "public" / "assets"
ITEMS = [
    {
        "productId": "viet-phuc-ao-tac-nu-demo",
        "productName": "Áo tấc nữ tham khảo",
        "category": "vietnamese-traditional/ao-tac",
        "file": "ao-tac-nu-tham-khao",
        "url": "https://upload.wikimedia.org/wikipedia/commons/thumb/6/62/Rio_m%C3%A3_ch%C3%A2u_%C3%A1o_t%E1%BA%A5c.jpg/500px-Rio_m%C3%A3_ch%C3%A2u_%C3%A1o_t%E1%BA%A5c.jpg",
        "sourcePage": "https://commons.wikimedia.org/wiki/File:Rio_m%C3%A3_ch%C3%A2u_%C3%A1o_t%E1%BA%A5c.jpg",
        "creator": "Ptdtch",
        "license": "CC BY-SA 4.0",
        "licenseUrl": "https://creativecommons.org/licenses/by-sa/4.0/",
    },
    {
        "productId": "ao-ba-ba-nu-demo",
        "productName": "Áo bà ba nữ",
        "category": "vietnamese-traditional/ao-ba-ba",
        "file": "ao-ba-ba-mekong",
        "url": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Vietnam_2011.jpg/500px-Vietnam_2011.jpg",
        "sourcePage": "https://commons.wikimedia.org/wiki/File:Vietnam_2011.jpg",
        "creator": "4bangoai",
        "license": "CC BY-SA 4.0",
        "licenseUrl": "https://creativecommons.org/licenses/by-sa/4.0/",
    },
    {
        "productId": "ao-dai-nu-co-dien-demo",
        "productName": "Áo dài nữ cổ điển",
        "category": "vietnamese-traditional/ao-dai",
        "file": "ao-dai-ho-guom",
        "url": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/%C3%81o_d%C3%A0i%2C_H%E1%BB%93_G%C6%B0%C6%A1m.jpg/500px-%C3%81o_d%C3%A0i%2C_H%E1%BB%93_G%C6%B0%C6%A1m.jpg",
        "sourcePage": "https://commons.wikimedia.org/wiki/File:%C3%81o_d%C3%A0i%2C_H%E1%BB%93_G%C6%B0%C6%A1m.jpg",
        "creator": "tq2cute",
        "license": "CC BY 2.0",
        "licenseUrl": "https://creativecommons.org/licenses/by/2.0/",
    },
]


def main() -> None:
    results = []
    for item in ITEMS:
        folder = ASSET_ROOT / "products" / item["category"] / item["productId"]
        folder.mkdir(parents=True, exist_ok=True)
        source_file = folder / "source.jpg"
        output_file = folder / "main.webp"

        request = urllib.request.Request(
            item["url"], headers={"User-Agent": "FashionShopCatalog/1.0 (image catalog verification)"}
        )
        with urllib.request.urlopen(request, timeout=30) as response:
            if response.status != 200:
                raise RuntimeError(f"Download failed: HTTP {response.status}: {item['url']}")
            source_file.write_bytes(response.read())

        with Image.open(source_file) as image:
            image.verify()
        with Image.open(source_file) as image:
            image = image.convert("RGB")
            image.thumbnail((720, 960), Image.Resampling.LANCZOS)
            image.save(output_file, "WEBP", quality=82, method=6)
            width, height = image.size

        data = output_file.read_bytes()
        item_result = {
            **item,
            "modifications": "Downloaded from the listed Commons thumbnail, resized within 720x960, converted from JPEG to WebP.",
            "localImage": "/assets/" + output_file.relative_to(ASSET_ROOT).as_posix(),
            "localSource": "/assets/" + source_file.relative_to(ASSET_ROOT).as_posix(),
            "format": Image.open(output_file).format,
            "width": width,
            "height": height,
            "bytes": len(data),
            "sha256": hashlib.sha256(data).hexdigest(),
            "verified": True,
        }
        results.append(item_result)

    manifest = ASSET_ROOT / "vietnamese-batch-01-manifest.json"
    manifest.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Downloaded and verified {len(results)} unique images: {manifest.relative_to(ROOT)}")
    for result in results:
        print(f"{result['productId']}: {result['width']}x{result['height']}, {result['bytes']} bytes, {result['localImage']}")


if __name__ == "__main__":
    main()
