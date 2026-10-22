# Dataset sample attribution and scope

- Dataset: Fashion Product Images (Small)
- Dataset publisher: Param Aggarwal
- Dataset page: https://www.kaggle.com/paramaggarwal/fashion-product-images-small
- License displayed on the Kaggle dataset page: MIT
- Source archive supplied by the project owner: `archive.zip`
- Included subset: 1,000 unique source IDs, selected deterministically. The supplied small archive contains 60x80 JPEG thumbnails; 509 Myntra sample files in this checkout still use AI-upscaled versions of those thumbnails.
- Metadata retained in `manifest.json`: source ID, product display name, article type, color, gender, season and usage.
- Fashion Shop generated fields: category mapping, demo price, sample stock and size options. These are not original source facts or live offers.

The archive itself did not contain a license or per-image author metadata. Published descriptions identify the photos and product records as originating from Myntra. Myntra's current terms restrict copying or distributing platform content without written permission: https://www.myntraglobal.com/pages/terms-of-use. The Kaggle dataset page's MIT label is attributed to the dataset publisher and is not independent evidence that the publisher could license every underlying Myntra image or mark.

This subset is included as a development/demo fixture at the project owner's request. Do not present it as Fashion Shop inventory or deploy it for commercial product listings unless the rights to the underlying images and product content have been confirmed. For production, replace it with owned photography or assets licensed for that use.

The ZIP did not include a `LICENSE` file or copyright notice. The Kaggle page currently displays MIT; retain that source attribution, and obtain the exact notice from the dataset publisher before redistributing the subset. This notice records provenance; it does not expand the rights granted by any upstream image owner.

## High-resolution replacements

- 500 image paths across `myntra-sample` (491) and `seed-catalog` (9) were replaced with source images from the [high-resolution Fashion Product Images mirror](https://huggingface.co/datasets/PestoRosso/lamoda-fashion-product-images). The mirror states that every image has a shortest edge of at least 640 pixels and displays an MIT license. Existing file paths and names are unchanged.
- The replacements are tracked in `../../../../../database/catalog_hires_replacement_map.csv`: 46 images use the exact source product ID and 454 use an image with the same article type and color. The latter are similar products, not the exact named SKU.
- The high-resolution mirror contains the same underlying Myntra product catalog. Its license label does not remove the upstream rights caveat already described above; verify image rights before commercial use.
