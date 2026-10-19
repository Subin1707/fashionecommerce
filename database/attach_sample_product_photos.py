"""Replace shared category placeholders on the 50 Shopee-style demo products.

Run from the repository root with Python and psycopg installed. The update is
limited to product image rows that still point at /images/catalog/ placeholders.
"""

import os

import psycopg


PHOTOS = {
    109: "https://cdn.hstatic.net/products/1000026602/img_1469_4a4f4e29d1a546f1b3ab2715a3fe1f43_master.jpg",  # compact tee
    110: "https://guco.vn/UploadImages/Data/News/8836/ao-so-mi-tay-dai-trang-A523-4.jpg",  # linen shirt
    111: "https://cdn.store-assets.com/s/1325208/i/95389760.jpeg",  # square-neck blouse
    112: "https://sakurafashion.vn/upload/sanpham/large/81692-ao-len-cardigan-mong-mua-he-cho-nu-1.jpg",  # cardigan
    113: "https://salt.tikicdn.com/cache/550x550/ts/product/14/b1/79/4ebc7ace18f6acdacbb00ecd0209a22b.jpg",  # linen set
    114: "https://down-vn.img.susercontent.com/file/vn-11134207-7qukw-lfcl37oor69wdb",  # jeans
    116: "https://pos.nvncdn.com/d0f3ca-7136/ps/20250109_GaFzo4OcRI.jpeg?v=1736401344",  # pleated skirt
    117: "https://cdn.hstatic.net/products/1000197303/pro_hoa_02_2_8c69674181864641906a98bec7a1d6e3.jpg",  # floral midi dress
    118: "https://product.hstatic.net/1000406613/product/z5350004102661_37fdfefca3d1f6030c7d1e619ab722ff_c50a290a6a8045a6b65822c83bcaab76.jpg",  # linen maxi
    119: "https://aodainetdepviet.com/cdn/shop/files/g1_2d408022-e586-4575-b153-e1655dafffc0.jpg?v=1729315486",  # Vietnamese ao dai
    120: "https://image.voh.com.vn/voh/Image/2019/10/29/chatlieu1_20191029045815.jpg?q=85&t=o&w=1600",  # ao ba ba
    121: "https://product.hstatic.net/1000304105/product/ao-khoac-1-lop-jeans-xcn3-t6121j8045__2__06c6d0ddf00142608ebdf5e2e645d408.jpg",  # denim jacket
    122: "https://down-vn.img.susercontent.com/file/vn-11134201-23030-js4jl67n5xovca",  # sports bra
    123: "https://product.hstatic.net/200000872231/product/quan_tap_legging_basic_livan_lv23210003__34__fcc37bd0c82b484abc7f53a6c5dc334b_1024x1024.png",  # leggings
    124: "https://bizweb.dktcdn.net/100/484/513/products/patch-co-xanh.jpg?v=1767771780430",  # pique polo
    125: "https://media.routine.vn/1200x1500/prod/product/10s25shs002-grey-1-jpg-ykmf.webp",  # Oxford shirt
    126: "https://cdn.hstatic.net/products/200000738343/87_34de26089df14a35b4545fed27a5b779_master.jpg",  # men's linen shirt
    127: "https://zumifashion.com/Uploads/DSCF5040.png",  # oversized tees
    128: "https://cdn.hstatic.net/products/1000096703/1_aca1a6fd100942e2add563c65b888932.jpg",  # tailored trousers
    129: "https://product.hstatic.net/1000378223/product/xanh_dam_truoc_0a67e6341be44622890b5a3d6ab1c364.jpg",  # men's jeans
    130: "https://unleashedvn.com/media/cong/OkQeYmJ4tiLCJOzQc9K9WU8JxonEpv1GWtHY8rAB.webp",  # khaki trousers
    131: "https://4menshop.com/images/thumbs/2021/02/quan-short-kaki-tron-qs009-15966-slide-products-6018ca1994e38.png",  # cotton shorts
    132: "https://down-vn.img.susercontent.com/file/vn-11134207-7r98o-lpkhx6u83044e0",  # blazer
    133: "https://viettien.vn/UploadImages/shops/anh-san-pham/SMART-CASUAL/QUAN-LOT/6R9706/6R9706a.jpg",  # cotton underwear
    134: "https://s.alicdn.com/@sc04/kf/Ad3b2fa4a06124fbb91973984ea8e8f1a0/MADE-IN-VIETNAM-simple-sporty-tank-top-for-men.png",  # gym top
    135: "https://vietrunning.vn/uploads/340/ao_t-_shirt.png-large.webp",  # running shirt
    136: "https://prodapi.hoaigiangshop.com/rm/file/media/public/IMG-0833-1/IMG-0833-1.jpeg",  # men's brocade ao dai
    137: "https://chus.vn/images/detailed/299/2_k4tf-wj.jpg",  # Vietnamese canvas tote
    138: "https://s.alicdn.com/%40sc04/kf/Aaaac9cb22b6842699e3a4d8f495fd4abz/Classic-Canvas-Unisex-Backpack-Eco-Friendly-Customized-OEM-ODM-Soft-Design-Wholesale-From-Vietnamese-Manufacturer.jpg",  # canvas backpack
    139: "https://product.hstatic.net/200000315153/product/vi-nam_238e222ccd1d4b1aa937414c0c2ca9d7.jpg",  # bifold wallet
    140: "https://down-vn.img.susercontent.com/file/vn-11134207-820l4-mewgpcgdj402d2",  # bucket hats
    141: "https://cdn.hstatic.net/products/200000689681/1_e1d7f8ded46c4038a23d0baa6912248c_grande.png",  # acetate sunglasses
    142: "https://ddreamer.com.vn/cdn/shop/files/silverjewelry-ddreamerjewelry-0484.jpg?v=1749312950",  # lotus necklace
    143: "https://chus.vn/images/detailed/248/10710_07_F5.jpg",  # silk scarf
    144: "https://down-vn.img.susercontent.com/file/vn-11134207-7ra0g-m92sct4ar81u4b",  # cotton socks
    145: "/images/catalog/sneaker-vai-trang.svg",  # lightweight local fallback for unreliable canvas sneaker photo
    146: "https://cdn.hstatic.net/products/1000230642/hsw011000xdg__6__72a30cb8afc34154855eefff4cb88b05_large.jpg",  # cushioned running shoes
    147: "https://product.hstatic.net/1000347349/product/giay-nam-scorpion-28me.16-4_694c8f3f5157496cb42f09fa777df949_master.jpg",  # leather loafers
    148: "https://mediaelly.sgp1.digitaloceanspaces.com/uploads/2024/03/22092831/giay-nu-thoi-trang-elly-eg254-13-1-1024x1024.jpg",  # block heels
    149: "https://bizweb.dktcdn.net/100/256/381/products/44.jpg?v=1660562108017",  # ballet flats
    150: "https://res.cloudinary.com/dck3z83wk/image/upload/v1768532689/dabao_leather_products/Zstrappy-brown-main-10de57b7-c14c-4268-ae55-3564998aef41.png",  # Hoi An leather sandals
    151: "https://product.hstatic.net/1000230642/product/bpw004288nau__6__8fdeddba76eb4f57a5afb8bcf0c2dd8c.jpg",  # soft slide sandals
    152: "https://content.pancake.vn/1/s1950x3380/fwebp/88/97/ac/1a/7148e743b8e466832dbd0f24a59bae855ad5cb00af06c164c5659938-w%3A2564-h%3A2564-l%3A1755831-t%3Aimage/jpeg.jpeg",  # ankle boots
    153: "https://bizweb.dktcdn.net/100/416/325/products/g1572-7.jpg?v=1730455837293",  # leather boots
    154: "https://product.hstatic.net/1000230642/product/gbsw01800kem__2__a4c3c2101dd14794afc7d0cea998bd60.jpg",  # crescent crossbody bag
    155: "https://product.hstatic.net/1000260559/product/that-lung-da-bo-khoa-cai-tien-loi-4290845__4__20ac7092dca74e0b9227728cbd4882c2.jpg",  # leather belt
    156: "https://n7media.coolmate.me/uploads/April2025/bra-chay-bo-phoi-luoi-medium-support-1872-den.jpg?aio=w-1100",  # modal bra
    157: "https://down-vn.img.susercontent.com/file/a53c7095f3bc74fbed885ef8ba62e387",  # satin underwear
    158: "https://cdn.hstatic.net/products/1000058447/00.com_vang_ml_paltal_bltn_194_4320_bolungmacnhacaocap_5911e3d9316d4ea3bbb192aff67764f0.png",  # cotton set
}


def main():
    if len(PHOTOS) != 49 or len(set(PHOTOS.values())) != 49:
        raise ValueError("Expected 49 active sample products with 49 unique image URLs")

    conninfo = os.getenv(
        "DATABASE_URL",
        "postgresql://fashion:fashion@localhost:5432/fashion",
    )
    with psycopg.connect(conninfo) as conn, conn.cursor() as cur:
        changed = 0
        for product_id, image_url in PHOTOS.items():
            cur.execute(
                """UPDATE product_images
                   SET image_url = %s
                   WHERE product_id = %s
                     AND image_url LIKE '/images/catalog/%%'""",
                (image_url, product_id),
            )
            changed += cur.rowcount
        if changed != 49:
            raise RuntimeError(f"Expected to update 49 placeholder images; updated {changed}")
    print(f"Updated {changed} demo product images; all 49 URLs are unique.")


if __name__ == "__main__":
    main()
