import { useCallback, useEffect, useMemo, useState } from 'react'
import { AdminProductForm } from '../../components/admin/AdminProductForm'
import * as adminService from '../../services/adminService'
import * as brandService from '../../services/brandService'
import * as categoryService from '../../services/categoryService'
import { initialProductForm } from '../../utils/constants'
import { formatCurrency } from '../../utils/formatCurrency'

const statusLabels = {
  ACTIVE: 'Đang bán',
  INACTIVE: 'Tạm ẩn',
}

export function ProductManagementPage({ setNotice }) {
  const [products, setProducts] = useState([])
  const [brands, setBrands] = useState([])
  const [categories, setCategories] = useState([])
  const [editingProduct, setEditingProduct] = useState(null)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)

  const loadProducts = useCallback(async function loadProducts() {
    setLoading(true)
    try {
      const [productRows, brandRows, categoryRows] = await Promise.all([
        adminService.getAdminProducts(),
        brandService.getBrands(),
        categoryService.getCategories(),
      ])
      setProducts(productRows)
      setBrands(Array.isArray(brandRows) ? brandRows : [])
      setCategories(Array.isArray(categoryRows) ? categoryRows : [])
    } catch (error) {
      setNotice?.(`Chưa đọc được sản phẩm quản trị: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }, [setNotice])

  useEffect(() => {
    Promise.resolve().then(loadProducts)
  }, [loadProducts])

  const filteredProducts = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return products.filter((product) => {
      const matchesQuery = !normalizedQuery
        || product.name?.toLowerCase().includes(normalizedQuery)
        || product.brandName?.toLowerCase().includes(normalizedQuery)
        || product.categoryName?.toLowerCase().includes(normalizedQuery)
        || String(product.id).includes(normalizedQuery)
      const matchesStatus = statusFilter === 'ALL' || product.status === statusFilter
      return matchesQuery && matchesStatus
    })
  }, [products, query, statusFilter])

  const summary = useMemo(() => ({
    total: products.length,
    active: products.filter((product) => product.status === 'ACTIVE').length,
    hidden: products.filter((product) => product.status === 'INACTIVE').length,
    lowStock: products.filter((product) => Number(product.totalStock || 0) <= 10).length,
  }), [products])

  function startCreate() {
    setEditingProduct({ ...initialProductForm, images: [] })
  }

  function startEdit(product) {
    const variants = product.variants || []

    const realVariants = variants.filter(
      (variant) =>
        !(
          String(variant.color || '').toUpperCase() === 'DEFAULT' &&
          String(variant.size || '').toUpperCase() === 'DEFAULT'
        ),
    )

    const defaultVariant = variants.find(
      (variant) =>
        String(variant.color || '').toUpperCase() === 'DEFAULT' &&
        String(variant.size || '').toUpperCase() === 'DEFAULT',
    )

    setEditingProduct({
      ...initialProductForm,
      ...product,
      brandId: product.brandId || '',
      categoryId: product.categoryId || '',
      salePrice: product.salePrice ?? '',
      hasVariants: realVariants.length > 0,
      stockQty: defaultVariant?.stockQty ?? (realVariants.length === 0 ? (product.totalStock ?? 0) : 0),
      defaultVariantId: defaultVariant?.id || null,
      variants:
        realVariants.length > 0
          ? realVariants.map((variant) => ({
              id: variant.id,
              sku: variant.sku,
              color: variant.color,
              size: variant.size,
              stockQty: variant.stockQty,
              priceAdjustment: variant.priceAdjustment ?? 0,
              isActive: variant.isActive !== false,
            }))
          : [],
      images: normalizeProductImages(product.images),
    })
  }

  async function updateProduct(event) {
    event.preventDefault()
    const payload = toProductPayload(editingProduct)
    const validationMessage = validateProductPayload(payload)
    if (validationMessage) {
      setNotice?.(validationMessage)
      return
    }

    try {
      await adminService.updateAdminProduct(editingProduct.id, payload)

      if (editingProduct.hasVariants) {
        for (const variant of editingProduct.variants || []) {
          /*
           * Variant có id = đã tồn tại
           */
          if (variant.id) {
            await adminService.updateAdminProductVariant(
              variant.id,
              {
                stockQty: Number(variant.stockQty),
                priceAdjustment: Number(variant.priceAdjustment) || 0,
                isActive: variant.isActive !== false,
              },
            )
          } else if (variant.color?.trim() && variant.size?.trim()) {
            /*
             * Variant chưa có id = admin vừa thêm mới
             */
            await adminService.createAdminProductVariant(
              editingProduct.id,
              {
                color: variant.color.trim(),
                size: variant.size.trim(),
                stockQty: Number(variant.stockQty) || 0,
                priceAdjustment: Number(variant.priceAdjustment) || 0,
              },
            )
          }
        }
      } else {
        // Sản phẩm không biến thể: cập nhật SKU mặc định
        if (editingProduct.defaultVariantId) {
          await adminService.updateAdminProductVariant(
            editingProduct.defaultVariantId,
            {
              stockQty: Number(editingProduct.stockQty) || 0,
              priceAdjustment: 0,
              isActive: true,
            },
          )
        }
      }

      setEditingProduct(null)
      setNotice?.(`Đã cập nhật sản phẩm #${editingProduct.id}.`)
      loadProducts()
    } catch (error) {
      setNotice?.(`Không cập nhật được sản phẩm: ${error.message}`)
    }
  }

  async function createProduct(event) {
    event.preventDefault()
    const payload = toProductPayload(editingProduct)
    const validationMessage = validateProductPayload(payload)
    if (validationMessage) {
      setNotice?.(validationMessage)
      return
    }

    try {
      const created = await adminService.createAdminProduct(payload)
      setEditingProduct(null)
      setNotice?.(`Đã tạo sản phẩm #${created.id}.`)
      loadProducts()
    } catch (error) {
      setNotice?.(`Không tạo được sản phẩm: ${error.message}`)
    }
  }

  async function hideProduct(product) {
    if (!window.confirm(`Tạm ẩn sản phẩm "${product.name}" khỏi trang bán hàng?`)) return
    try {
      await adminService.updateAdminProduct(product.id, { status: 'INACTIVE' })
      setNotice?.(`Đã tạm ẩn sản phẩm #${product.id}. Dữ liệu sản phẩm vẫn được giữ lại.`)
      loadProducts()
    } catch (error) {
      setNotice?.(`Không tạm ẩn được sản phẩm: ${error.message}`)
    }
  }

  async function restoreProduct(product) {
    try {
      await adminService.updateAdminProduct(product.id, { status: 'ACTIVE' })
      setNotice?.(`Đã hiện lại sản phẩm #${product.id}.`)
      loadProducts()
    } catch (error) {
      setNotice?.(`Không hiện lại được sản phẩm: ${error.message}`)
    }
  }

  async function removeProduct(product) {
    if (!window.confirm(`Xóa vĩnh viễn sản phẩm "${product.name}"? Hành động này không thể hoàn tác.`)) return
    try {
      await adminService.deleteAdminProduct(product.id)
      setNotice?.(`Đã xóa sản phẩm #${product.id}.`)
      loadProducts()
    } catch (error) {
      setNotice?.(`Không xóa được sản phẩm: ${error.message}`)
    }
  }

  return (
    <section className="admin-product-page">
      <div className="admin-product-hero">
        <div>
          <p className="eyebrow">Kho sản phẩm</p>
          <h1>Quản lý sản phẩm</h1>
          <p>Theo dõi trạng thái bán, tồn kho, sản phẩm nổi bật và chỉnh sửa nhanh thông tin catalog.</p>
        </div>
        <button type="button" onClick={startCreate}>+ Thêm sản phẩm</button>
      </div>

      <div className="admin-product-metrics">
        <Metric label="Tổng mẫu" value={summary.total} />
        <Metric label="Đang bán" value={summary.active} />
        <Metric label="Tạm ẩn" value={summary.hidden} />
        <Metric label="Tồn kho thấp" value={summary.lowStock} tone="warning" />
      </div>

      <div className="admin-product-layout">
        <div className="admin-product-list-panel">
          <div className="admin-product-toolbar">
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm theo tên, thương hiệu, danh mục, mã sản phẩm..."
            />
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang bán</option>
              <option value="INACTIVE">Tạm ẩn</option>
            </select>
            <button type="button" className="ghost" onClick={loadProducts} disabled={loading}>
              {loading ? 'Đang tải...' : 'Làm mới'}
            </button>
          </div>

          {loading && <div className="admin-product-loading" />}
          {!loading && filteredProducts.length === 0 && <p className="empty-state">Chưa có sản phẩm phù hợp.</p>}

          <div className="admin-product-card-grid">
            {!loading && filteredProducts.map((product) => (
              <AdminProductCard
                key={product.id}
                product={product}
                selected={editingProduct?.id === product.id}
                onEdit={startEdit}
                onHide={hideProduct}
                onRestore={restoreProduct}
                onRemove={removeProduct}
              />
            ))}
          </div>
        </div>

        <aside className="admin-product-editor-panel">
          {editingProduct ? (
            <AdminProductForm
              value={editingProduct}
              onChange={setEditingProduct}
              onSubmit={editingProduct.id ? updateProduct : createProduct}
              brands={brands}
              categories={categories}
              title={editingProduct.id ? `Sửa sản phẩm #${editingProduct.id}` : 'Thêm sản phẩm'}
              submitLabel={editingProduct.id ? 'Lưu thay đổi' : 'Tạo sản phẩm'}
            />
          ) : (
            <div className="admin-product-empty-editor">
              <p className="eyebrow">Biểu mẫu</p>
              <h2>Chọn một sản phẩm để chỉnh sửa</h2>
              <p>Hoặc bấm thêm sản phẩm để tạo mẫu mới cho catalog.</p>
              <button type="button" onClick={startCreate}>Thêm sản phẩm</button>
            </div>
          )}
        </aside>
      </div>
    </section>
  )
}

function AdminProductCard({ product, selected, onEdit, onHide, onRestore, onRemove }) {
  const price = product.salePrice || product.basePrice
  const hasSale = product.salePrice && product.basePrice && product.salePrice < product.basePrice
  const stock = Number(product.totalStock || 0)
  const stockTone = stock === 0 ? 'soldout' : stock <= 10 ? 'low' : 'available'
  const imageUrl = getAdminProductImage(product)
  const isHidden = product.status === 'INACTIVE'

  return (
    <article className={`admin-product-card ${selected ? 'selected' : ''}`}>
      <div className="admin-product-card-media">
        {imageUrl ? <img src={imageUrl} alt={product.name} /> : <span>Chưa có ảnh</span>}
      </div>

      <div className="admin-product-card-head">
        <div>
          <p className="eyebrow">#{product.id} - {product.brandName || 'Thương hiệu'}</p>
          <h2>{product.name}</h2>
        </div>
        <span className={`admin-product-status ${String(product.status || '').toLowerCase()}`}>
          {statusLabels[product.status] || product.status || 'Chưa rõ'}
        </span>
      </div>

      <p>{product.categoryName || 'Chưa có danh mục'} - {product.material || 'Chưa có chất liệu'} - {product.fit || 'Chưa có form'}</p>

      <div className="admin-product-card-meta">
        <span className={stockTone}>{stock} tồn kho</span>
        {product.isFeatured && <span>Nổi bật</span>}
        {product.isNew && <span>Hàng mới</span>}
      </div>

      <div className="admin-product-price-row">
        <strong>{formatCurrency(price)}</strong>
        {hasSale && <del>{formatCurrency(product.basePrice)}</del>}
      </div>

      <div className="admin-product-actions">
        <button type="button" onClick={() => onEdit(product)}>Sửa</button>
        {isHidden ? (
          <button type="button" onClick={() => onRestore(product)}>Hiện lại</button>
        ) : (
          <button type="button" onClick={() => onHide(product)}>Tạm ẩn</button>
        )}
        <button type="button" className="danger" onClick={() => onRemove(product)}>Xóa</button>
      </div>
    </article>
  )
}

function getAdminProductImage(product) {
  return product.images?.find((image) => image.isPrimary)?.imageUrl || product.images?.[0]?.imageUrl || ''
}

function Metric({ label, value, tone = '' }) {
  return (
    <article className={`admin-product-metric ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
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
    stockQty: !hasVariants ? Number(product.stockQty) : 0,
    variants: hasVariants
      ? (product.variants || []).map((variant) => ({
          id: variant.id,
          color: variant.color?.trim() || '',
          size: variant.size?.trim() || '',
          stockQty: Number(variant.stockQty) || 0,
          priceAdjustment:
            variant.priceAdjustment !== '' &&
            variant.priceAdjustment !== null &&
            variant.priceAdjustment !== undefined
              ? Number(variant.priceAdjustment)
              : 0,
        }))
      : [],
    images: sanitizeProductImages(product.images),
  }
}

function validateProductPayload(product) {
  if (!product.name?.trim()) return 'Vui lòng nhập tên sản phẩm.'
  if (!Number.isFinite(product.brandId) || product.brandId <= 0) return 'Vui lòng chọn thương hiệu.'
  if (!Number.isFinite(product.categoryId) || product.categoryId <= 0) return 'Vui lòng chọn danh mục.'
  if (!Number.isFinite(product.basePrice) || product.basePrice <= 0) return 'Giá bán phải lớn hơn 0.'
  if (product.salePrice != null && (!Number.isFinite(product.salePrice) || product.salePrice < 0)) {
    return 'Giá khuyến mãi không hợp lệ.'
  }

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

  for (let i = 0; i < product.variants.length; i += 1) {
    const variant = product.variants[i]
    if (!variant.color) return `Biến thể ${i + 1}: vui lòng nhập màu.`
    if (!variant.size) return `Biến thể ${i + 1}: vui lòng nhập size.`
    if (!Number.isFinite(variant.stockQty) || variant.stockQty < 0 || !Number.isInteger(variant.stockQty)) {
      return `Biến thể ${i + 1}: tồn kho không hợp lệ.`
    }
  }

  return null
}

function normalizeProductImages(images) {
  if (!Array.isArray(images)) return []

  return images.map((image, index) => ({
    id: image.id,
    variantId: image.variantId || null,
    imageUrl: image.imageUrl || '',
    altText: image.altText || '',
    isPrimary: Boolean(image.isPrimary),
    displayOrder: image.displayOrder ?? index + 1,
  }))
}

function sanitizeProductImages(images) {
  const validImages = normalizeProductImages(images)
    .filter((image) => image.imageUrl.trim())
    .map((image, index) => ({
      id: image.id || null,
      variantId: image.variantId || null,
      imageUrl: image.imageUrl.trim(),
      altText: image.altText?.trim() || null,
      isPrimary: Boolean(image.isPrimary),
      displayOrder: Number(image.displayOrder ?? index + 1),
    }))

  if (validImages.length > 0 && !validImages.some((image) => image.isPrimary)) {
    validImages[0] = { ...validImages[0], isPrimary: true }
  }

  return validImages
}
