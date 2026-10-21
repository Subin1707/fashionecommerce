"""Fetch licensed Commons references for accessories."""

from __future__ import annotations

import hashlib, json, re, sys, time, urllib.parse, urllib.request
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "frontend" / "public" / "assets"
ITEMS = [
    ("hat-straw-men", "Men's straw hat reference", "accessories/hats", "File:Mens straw sun hat.jpg"),
    ("hat-sun-caps", "Sun hats reference", "accessories/hats", "File:Sun hat caps.jpg"),
    ("hat-studio", "Sun hat reference", "accessories/hats", "File:Studio Sun-Hat (16273729209).jpg"),
    ("belt-leather-1", "Leather belt reference", "belts/leather", "File:Leather Belt.jpg"),
    ("belt-leather-2", "Leather belt and buckle reference", "belts/leather", "File:Leather belt.jpg"),
    ("belt-handmade", "Handmade leather belt reference", "belts/leather", "File:A Leather Belt.jpg"),
    ("scarf-soft", "Soft scarf reference", "accessories/scarves", "File:Scarf soft.jpg"),
    ("scarf-infinity", "Infinity scarf reference", "accessories/scarves", "File:Infinity Scarf.jpg"),
    ("scarf-cotton", "Cotton scarf reference", "accessories/scarves", "File:Bodo cotton scarf.jpg"),
    ("scarf-print", "Scarf reference", "accessories/scarves", "File:Scarf 120000.jpg"),
    ("sunglasses-basic", "Sunglasses reference", "accessories/eyewear", "File:Sunglasses.jpg"),
    ("sunglasses-shield", "Shield sunglasses reference", "accessories/eyewear", "File:Shield sunglasses.jpg"),
]
UA = "FashionShopImageCatalog/1.0 (local educational catalog; contact admin@fashion.local)"
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="backslashreplace")


def clean(meta: dict, key: str) -> str:
    return re.sub(r"<[^>]*>", " ", meta.get(key, {}).get("value", "")).strip()


def get_info(title: str) -> dict:
    query = urllib.parse.urlencode({"action":"query","titles":title,"prop":"imageinfo","iiprop":"url|extmetadata","iiurlwidth":"720","format":"json"})
    req = urllib.request.Request("https://commons.wikimedia.org/w/api.php?" + query, headers={"User-Agent":UA})
    with urllib.request.urlopen(req,timeout=30) as response:
        page=next(iter(json.load(response)["query"]["pages"].values()))
    if "imageinfo" not in page: raise RuntimeError("No imageinfo on Commons")
    return page["imageinfo"][0]


def main() -> None:
    results=[]
    for asset_id,label,category,title in ITEMS:
        try:
            info=get_info(title); meta=info.get("extmetadata",{})
            license_name,license_url=clean(meta,"LicenseShortName"),clean(meta,"LicenseUrl")
            if not license_name or not license_url: raise RuntimeError("Missing verifiable license metadata")
            folder=ASSET_ROOT/"products"/category/asset_id; folder.mkdir(parents=True,exist_ok=True)
            source,output=folder/"source.jpg",folder/"main.webp"
            request=urllib.request.Request(info.get("thumburl",info["url"]),headers={"User-Agent":UA})
            with urllib.request.urlopen(request,timeout=40) as response: source.write_bytes(response.read())
            with Image.open(source) as image: image.verify()
            with Image.open(source) as image:
                image=image.convert("RGB"); image.thumbnail((720,960),Image.Resampling.LANCZOS)
                image.save(output,"WEBP",quality=82,method=6); width,height=image.size
            data=output.read_bytes()
            results.append({"assetId":asset_id,"displayName":label,"category":category,"commonsTitle":title,
                "sourcePage":"https://commons.wikimedia.org/wiki/"+urllib.parse.quote(title.replace(" ","_"),safe=":_()'.,-"),
                "creator":clean(meta,"Artist") or clean(meta,"Credit") or "See Commons source page",
                "license":license_name,"licenseUrl":license_url,"modifications":"Commons thumbnail resized within 720x960, converted to WebP; source retained as source.jpg.",
                "localImage":"/assets/"+output.relative_to(ASSET_ROOT).as_posix(),"localSource":"/assets/"+source.relative_to(ASSET_ROOT).as_posix(),
                "format":"WEBP","width":width,"height":height,"bytes":len(data),"sha256":hashlib.sha256(data).hexdigest(),"verified":True})
            print(f"OK {title}: {license_name}, {width}x{height}, {len(data)} bytes")
        except Exception as error: print(f"SKIP {title}: {error}")
        time.sleep(0.4)
    target=ASSET_ROOT/"vietnamese-batch-08-manifest.json"
    target.write_text(json.dumps(results,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(f"Downloaded {len(results)}/{len(ITEMS)}; manifest {target.relative_to(ROOT)}")


if __name__=="__main__": main()
