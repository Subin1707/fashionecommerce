# Dataset sample attribution and scope

- Dataset: Fashion Product Images (Small)
- Dataset publisher: Param Aggarwal
- Dataset page: https://www.kaggle.com/paramaggarwal/fashion-product-images-small
- License displayed on the Kaggle dataset page: MIT
- Source archive supplied by the project owner: `archive.zip`
- Included subset: 1,000 unique source IDs, selected deterministically; source image files are unchanged 60×80 JPEGs.
- Metadata retained in `manifest.json`: source ID, product display name, article type, color, gender, season and usage.
- Fashion Shop generated fields: category mapping, demo price, sample stock and size options. These are not original source facts or live offers.

The archive itself did not contain a license or per-image author metadata. Published descriptions identify the photos and product records as originating from Myntra. Myntra's current terms restrict copying or distributing platform content without written permission: https://www.myntraglobal.com/pages/terms-of-use. The Kaggle dataset page's MIT label is attributed to the dataset publisher and is not independent evidence that the publisher could license every underlying Myntra image or mark.

This subset is included as a development/demo fixture at the project owner's request. Do not present it as Fashion Shop inventory or deploy it for commercial product listings unless the rights to the underlying images and product content have been confirmed. For production, replace it with owned photography or assets licensed for that use.

The ZIP did not include a `LICENSE` file or copyright notice. The Kaggle page currently displays MIT; retain that source attribution, and obtain the exact notice from the dataset publisher before redistributing the subset. This notice records provenance; it does not expand the rights granted by any upstream image owner.
