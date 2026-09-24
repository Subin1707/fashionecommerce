import { useEffect, useState } from 'react'
import { AdminProductForm } from '../../components/admin/AdminProductForm'
import * as adminService from '../../services/adminService'
import * as brandService from '../../services/brandService'
import * as categoryService from '../../services/categoryService'
import { initialProductForm } from '../../utils/constants'

export function ProductFormPage({ setNotice }) {
  const [form, setForm] = useState(initialProductForm)
  const [brands, setBrands] = useState([])
  const [categories, setCategories] = useState([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    Promise.all([
      brandService.getBrands(),
      categoryService.getCategories(),
    ])
      .then(([brandRows, categoryRows]) => {
        setBrands(Array.isArray(brandRows) ? brandRows : [])
        setCategories(Array.isArray(categoryRows) ? categoryRows : [])
      })
      .catch((error) =>
        setNotice?.(`Chưa tải được thương hiệu/danh mục: ${error.message}`)
      )
  }, [setNotice])

  async function submit(event) {
    event.preventDefault()

    const payload = toProductPayload(form)
    const error = validateProductPayload(payload)

    if (error) {
      setNotice?.(error)
      return
    }

    setSaving(true)

    try {
      const product = await adminService.createAdminProduct(payload)

      setForm(initialProductForm)

      setNotice?.(
        `Đã tạo sản phẩm quản trị #${product.id}.`
      )
    } catch (error) {
      setNotice?.(`Không tạo được sản phẩm: ${error.message}`)
    } finally {
      setSaving(false)
    }
  }

  return (
    <AdminProductForm
      value={form}
      onChange={setForm}
      onSubmit={submit}
      title="Thêm sản phẩm"
      submitLabel="Tạo sản phẩm"
      brands={brands}
      categories={categories}
      saving={saving}
    />
  )
}

function toProductPayload(product) {
  const hasVariants = Boolean(product.hasVariants)

  return {
    brandId: Number(product.brandId),
    categoryId: Number(product.categoryId),

    name: product.name?.trim() || '',
    description: product.description?.trim() || '',

    basePrice: Number(product.basePrice),

    salePrice:
      product.salePrice !== '' &&
      product.salePrice !== null &&
      product.salePrice !== undefined
        ? Number(product.salePrice)
        : null,

    material: product.material?.trim() || '',
    fit: product.fit?.trim() || '',
    gender: product.gender || 'UNISEX',
    status: product.status || 'ACTIVE',

    isFeatured: Boolean(product.isFeatured),
    isNew: Boolean(product.isNew),

    /*
     * Không có biến thể:
     * gửi stockQty.
     */
    stockQty: !hasVariants
      ? Number(product.stockQty)
      : 0,

    /*
     * Có biến thể:
     * gửi variants[].
     */
    variants: hasVariants
      ? (product.variants || []).map((variant) => ({
          color: variant.color?.trim() || '',
          size: variant.size?.trim() || '',

          stockQty: Number(variant.stockQty),

          priceAdjustment:
            variant.priceAdjustment !== '' &&
            variant.priceAdjustment !== null &&
            variant.priceAdjustment !== undefined
              ? Number(variant.priceAdjustment)
              : 0,
        }))
      : [],

    images: normalizeImages(product.images),
  }
}

function normalizeImages(images) {
  const validImages = (Array.isArray(images) ? images : [])
    .filter((image) => image.imageUrl?.trim())
    .map((image, index) => ({
      id: image.id || null,
      variantId: image.variantId || null,
      imageUrl: image.imageUrl.trim(),
      altText: image.altText?.trim() || null,
      isPrimary: Boolean(image.isPrimary),
      displayOrder: Number(image.displayOrder ?? index + 1),
    }))

  if (
    validImages.length > 0 &&
    !validImages.some((image) => image.isPrimary)
  ) {
    validImages[0] = {
      ...validImages[0],
      isPrimary: true,
    }
  }

  return validImages
}

function validateProductPayload(product) {
  if (!product.name?.trim()) {
    return 'Vui lòng nhập tên sản phẩm.'
  }

  if (!Number.isFinite(product.brandId) || product.brandId <= 0) {
    return 'Vui lòng chọn thương hiệu.'
  }

  if (
    !Number.isFinite(product.categoryId) ||
    product.categoryId <= 0
  ) {
    return 'Vui lòng chọn danh mục.'
  }

  if (
    !Number.isFinite(product.basePrice) ||
    product.basePrice <= 0
  ) {
    return 'Giá bán phải lớn hơn 0.'
  }

  if (
    product.salePrice != null &&
    (!Number.isFinite(product.salePrice) || product.salePrice < 0)
  ) {
    return 'Giá khuyến mãi không hợp lệ.'
  }

  /*
   * Sản phẩm không biến thể
   */
  if (!product.variants || product.variants.length === 0) {
    if (
      !Number.isFinite(product.stockQty) ||
      product.stockQty < 0 ||
      !Number.isInteger(product.stockQty)
    ) {
      return 'Vui lòng nhập số lượng tồn kho hợp lệ.'
    }

    return null
  }

  /*
   * Sản phẩm có biến thể
   */
  for (let i = 0; i < product.variants.length; i += 1) {
    const variant = product.variants[i]

    if (!variant.color) {
      return `Biến thể ${i + 1}: vui lòng nhập màu.`
    }

    if (!variant.size) {
      return `Biến thể ${i + 1}: vui lòng nhập size.`
    }

    if (
      !Number.isFinite(variant.stockQty) ||
      variant.stockQty < 0 ||
      !Number.isInteger(variant.stockQty)
    ) {
      return `Biến thể ${i + 1}: tồn kho không hợp lệ.`
    }
  }

  return null
}