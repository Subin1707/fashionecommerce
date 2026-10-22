# Vietnamese catalog images: unresolved local assets

Status: BLOCKED for image restoration. The three local paths below are referenced by `database/seed_vietnamese_catalog.sql`, but the expected files are absent from this checkout. No replacement image was selected.

| Product ID | Seed slug | Expected local path | Existing source evidence |
| --- | --- | --- | --- |
| 984 | `viet-phuc-ao-tac-nu-demo` | `/assets/products/vietnamese-traditional/ao-tac/viet-phuc-ao-tac-nu-demo/main.webp` | `database/download_vietnamese_catalog_images.py` maps this slug to Wikimedia `File:Rio m? ch?u ?o t?c.jpg`, creator Ptdtch, CC BY-SA 4.0. |
| 997 | `ao-ba-ba-nu-demo` | `/assets/products/vietnamese-traditional/ao-ba-ba/ao-ba-ba-nu-demo/main.webp` | The downloader maps this slug to Wikimedia `File:Vietnam 2011.jpg`, creator 4bangoai, CC BY-SA 4.0. |
| 990 | `ao-dai-nu-co-dien-demo` | `/assets/products/vietnamese-traditional/ao-dai/ao-dai-nu-co-dien-demo/main.webp` | The downloader maps this slug to Wikimedia `File:?o d?i, H? G??m.jpg`, creator tq2cute, CC BY 2.0. |

The attribution and source URLs are also recorded in the historical seed. The download script is present in commit `e4b76252f717f00143b46b6276d4ff5aa5952fef`. The files are not present under `frontend/public/assets/products/vietnamese-traditional`, and no corresponding manifest was found. Outbound HTTP checks to Wikimedia were blocked by the current Windows environment (`WinError 10013`), so current source-page availability and image content could not be independently revalidated in this run.

Next safe step: run `python database/download_vietnamese_catalog_images.py` in an environment with outbound access to Wikimedia, inspect the generated images and `frontend/public/assets/vietnamese-batch-01-manifest.json`, then verify the checked-in seed paths resolve to the downloaded files. This script creates source JPEG and optimized WebP files plus attribution metadata; review its generated artifacts before applying them. Keep the image-integrity test failing until those specific assets are verified and present.
