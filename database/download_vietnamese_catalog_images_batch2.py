"""Fetch individually licensed Vietnamese clothing images from Commons."""

from __future__ import annotations

import hashlib
import json
import re
import urllib.parse
import urllib.request
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "frontend" / "public" / "assets"
ITEMS = [
    ("ao-tu-than-photo", "Áo tứ thân (ảnh tham khảo)", "vietnamese-traditional/ao-tu-than", "File:Ao Tu Than.jpg"),
    ("ao-tu-than-sishenao-1", "Áo tứ thân (ảnh tham khảo)", "vietnamese-traditional/ao-tu-than", "File:Sishenao1.jpg"),
    ("ao-tu-than-sishenao-3", "Trang phục biểu diễn Việt (ảnh tham khảo)", "vietnamese-traditional/festival", "File:Sishenao3.jpg"),
    ("ao-ngu-than-archive", "Áo ngũ thân trong ảnh tư liệu", "vietnamese-traditional/ngu-than", "File:Femme annamite coiffure tonkin.jpg"),
    ("ao-ngu-than-postcard-1904", "Áo ngũ thân trong bưu ảnh 1904", "vietnamese-traditional/ngu-than", "File:Ao ngu than on postcard dated 1904.JPG"),
    ("nhat-binh-vi-kim-ngoc", "Áo Nhật Bình trong ảnh tư liệu", "vietnamese-traditional/nhat-binh", "File:Vietnamese woman wearing Áo Nhật Bình.jpg"),
    ("nhat-binh-nam-phuong", "Áo Nhật Bình trong ảnh Nam Phương", "vietnamese-traditional/nhat-binh", "File:Nam Phương empress of Vietnam.jpg"),
    ("ao-ba-ba-aobaba158", "Áo bà ba (ảnh tham khảo)", "vietnamese-traditional/ao-ba-ba", "File:AOBABA158.jpg"),
    ("ao-ba-ba-aobaba176", "Áo bà ba (ảnh tham khảo)", "vietnamese-traditional/ao-ba-ba", "File:AOBABA176.jpg"),
    ("ao-ba-ba-1955", "Áo bà ba trong ảnh tư liệu 1955", "vietnamese-traditional/ao-ba-ba", "File:Áo bà ba 1955.jpg"),
    ("ao-dai-vietnamese", "Áo dài Việt Nam (ảnh tham khảo)", "vietnamese-traditional/ao-dai", "File:Vietnamese áo dài.jpg"),
    ("ao-dai-woman", "Áo dài nữ (ảnh tham khảo)", "vietnamese-traditional/ao-dai", "File:Woman wearing Ao Dai.jpg"),
    ("ao-dai-young-lady", "Áo dài nữ (ảnh tham khảo)", "vietnamese-traditional/ao-dai", "File:A Vietnamese young lady in ao dai (cropped).jpg"),
]


def api_image_info(title: str) -> dict:
    query = urllib.parse.urlencode({
        "action": "query",
        "titles": title,
        "prop": "imageinfo",
        "iiprop": "url|extmetadata",
        "iiurlwidth": "720",
        "format": "json",
    })
    request = urllib.request.Request(
        f"https://commons.wikimedia.org/w/api.php?{query}",
        headers={"User-Agent": "FashionShopImageCatalog/1.0 (local catalog; contact: admin@fashion.local)"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        data = json.load(response)
    page = next(iter(data["query"]["pages"].values()))
    if "imageinfo" not in page:
        raise RuntimeError(f"Commons has no image data for {title}")
    return page["imageinfo"][0]


def meta_value(meta: dict, key: str) -> str:
    value = meta.get(key, {}).get("value", "")
    return re.sub(r"<[^>]*>", " ", value).strip()


def main() -> None:
    results = []
    for asset_id, display_name, category, title in ITEMS:
        info = api_image_info(title)
        meta = info.get("extmetadata", {})
        license_name = meta_value(meta, "LicenseShortName")
        license_url = meta_value(meta, "LicenseUrl")
        if not license_name:
            raise RuntimeError(f"License could not be verified for {title}: {license_name!r}")
        if not license_url and license_name.casefold() == "public domain":
            license_url = "https://commons.wikimedia.org/wiki/Commons:Licensing"
        if not license_url:
            raise RuntimeError(f"License URL could not be verified for {title}: {license_name!r}")

        directory = ASSET_ROOT / "products" / category / asset_id
        directory.mkdir(parents=True, exist_ok=True)
        source_path = directory / "source.jpg"
        output_path = directory / "main.webp"
        source_url = info.get("thumburl", info["url"])
        request = urllib.request.Request(
            source_url,
            headers={"User-Agent": "FashionShopImageCatalog/1.0 (local catalog; contact: admin@fashion.local)"},
        )
        with urllib.request.urlopen(request, timeout=40) as response:
            if response.status != 200:
                raise RuntimeError(f"HTTP {response.status}: {source_url}")
            source_path.write_bytes(response.read())

        with Image.open(source_path) as image:
            image.verify()
        with Image.open(source_path) as image:
            image = image.convert("RGB")
            image.thumbnail((720, 960), Image.Resampling.LANCZOS)
            image.save(output_path, "WEBP", quality=82, method=6)
            width, height = image.size

        data = output_path.read_bytes()
        creator = meta_value(meta, "Artist") or meta_value(meta, "Credit") or "Unknown (see Commons page)"
        result = {
            "assetId": asset_id,
            "displayName": display_name,
            "category": category,
            "commonsTitle": title,
            "sourcePage": "https://commons.wikimedia.org/wiki/" + urllib.parse.quote(title.replace(" ", "_"), safe=":_()'.,-"),
            "creator": creator,
            "license": license_name,
            "licenseUrl": license_url,
            "modifications": "Downloaded Commons thumbnail, resized within 720x960, converted to WebP; original downloaded thumbnail retained as source.jpg.",
            "localImage": "/assets/" + output_path.relative_to(ASSET_ROOT).as_posix(),
            "localSource": "/assets/" + source_path.relative_to(ASSET_ROOT).as_posix(),
            "format": Image.open(output_path).format,
            "width": width,
            "height": height,
            "bytes": len(data),
            "sha256": hashlib.sha256(data).hexdigest(),
            "verified": True,
        }
        results.append(result)
        print(f"OK {title}: {license_name}, {width}x{height}, {len(data)} bytes")

    manifest = ASSET_ROOT / "vietnamese-batch-02-manifest.json"
    manifest.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Downloaded {len(results)} images to {manifest.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
