"""Fetch verified Commons reference images for footwear and small accessories."""

from __future__ import annotations

import hashlib, json, re, sys, time, urllib.parse, urllib.request
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "frontend" / "public" / "assets"
ITEMS = [
    ("boots-leather", "Leather boots reference", "shoes/boots", "File:Leather boots.jpg"),
    ("boots-brown", "Brown leather boot reference", "shoes/boots", "File:Leather boot.jpg"),
    ("boots-womens", "Women's leather boots reference", "shoes/boots", "File:Women's leather boots.jpg"),
    ("boots-high-heel", "Black heeled boots reference", "shoes/boots", "File:Black high-heeled boots in synthetic leather 1.jpg"),
    ("boots-black", "Black boots reference", "shoes/boots", "File:Black boots.jpg"),
    ("slippers-open", "Open heel slippers reference", "shoes/slippers", "File:Slippers.jpg"),
    ("slippers-house", "House slippers reference", "shoes/slippers", "File:Slippers Shoes.jpg"),
    ("slippers-flip-flops", "Slippers and flip flops reference", "shoes/slippers", "File:Slippers or Flip Flops.jpg"),
    ("slippers-market", "Slippers reference", "shoes/slippers", "File:All the slippers in the market.jpg"),
    ("socks-knit", "Knitted socks reference", "accessories/socks", "File:Hand knitted socks.jpg"),
    ("socks-cotton", "Cotton socks reference", "accessories/socks", "File:Premium Cotton Socks.jpg"),
    ("socks-anti-slip", "Anti slip socks reference", "accessories/socks", "File:Anti-slip socks.jpg"),
    ("hairband-plastic", "Simple hairband reference", "accessories/hair", "File:Plastic hairband.JPG"),
    ("hair-scrunchie", "Scrunchie reference", "accessories/hair", "File:Chouchou.jpg"),
    ("hair-accessory-array", "Hair accessories assortment reference", "accessories/hair", "File:A Colorful Array of Hair Accessories and Trinkets.jpg"),
]
UA = "FashionShopImageCatalog/1.0 (local educational catalog; contact admin@fashion.local)"
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="backslashreplace")

def clean(meta: dict, key: str) -> str:
    return re.sub(r"<[^>]*>", " ", meta.get(key, {}).get("value", "")).strip()

def get_info(title: str) -> dict:
    query=urllib.parse.urlencode({"action":"query","titles":title,"prop":"imageinfo","iiprop":"url|extmetadata","iiurlwidth":"720","format":"json"})
    req=urllib.request.Request("https://commons.wikimedia.org/w/api.php?"+query,headers={"User-Agent":UA})
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
            req=urllib.request.Request(info.get("thumburl",info["url"]),headers={"User-Agent":UA})
            with urllib.request.urlopen(req,timeout=40) as response: source.write_bytes(response.read())
            with Image.open(source) as image: image.verify()
            with Image.open(source) as image:
                image=image.convert("RGB"); image.thumbnail((720,960),Image.Resampling.LANCZOS)
                image.save(output,"WEBP",quality=82,method=6); width,height=image.size
            data=output.read_bytes()
            results.append({"assetId":asset_id,"displayName":label,"category":category,"commonsTitle":title,
                "sourcePage":"https://commons.wikimedia.org/wiki/"+urllib.parse.quote(title.replace(" ","_"),safe=":_()'.,-"),
                "creator":clean(meta,"Artist") or clean(meta,"Credit") or "See Commons source page",
                "license":license_name,"licenseUrl":license_url,"modifications":"Commons thumbnail resized within 720x960 and converted to WebP; source thumbnail retained as source.jpg.",
                "localImage":"/assets/"+output.relative_to(ASSET_ROOT).as_posix(),"localSource":"/assets/"+source.relative_to(ASSET_ROOT).as_posix(),
                "format":"WEBP","width":width,"height":height,"bytes":len(data),"sha256":hashlib.sha256(data).hexdigest(),"verified":True})
            print(f"OK {title}: {license_name}, {width}x{height}, {len(data)} bytes")
        except Exception as error: print(f"SKIP {title}: {error}")
        time.sleep(0.4)
    target=ASSET_ROOT/"vietnamese-batch-09-manifest.json"
    target.write_text(json.dumps(results,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(f"Downloaded {len(results)}/{len(ITEMS)}; manifest {target.relative_to(ROOT)}")

if __name__=="__main__": main()
