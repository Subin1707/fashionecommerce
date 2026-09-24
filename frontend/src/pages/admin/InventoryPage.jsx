import { useCallback, useEffect, useMemo, useState } from 'react'
import * as adminService from '../../services/adminService'
import { formatCurrency } from '../../utils/formatCurrency'

const emptyVariantForm = {
  color: '',
  size: '',
  stockQty: 0,
  priceAdjustment: 0,
}

const stockFilters = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'LOW', label: 'Sắp hết' },
  { value: 'OUT', label: 'Hết hàng' },
  { value: 'HEALTHY', label: 'Đủ hàng' },
]

export function InventoryPage({ setNotice }) {
  const [products, setProducts] = useState([])
  const [variants, setVariants] = useState([])
  const [selectedProduct, setSelectedProduct] = useState(null)
  const [variantForm, setVariantForm] = useState(emptyVariantForm)
  const [query, setQuery] = useState('')
  const [threshold, setThreshold] = useState(10)
  const [stockFilter, setStockFilter] = useState('LOW')
  const [loadingProducts, setLoadingProducts] = useState(true)
  const [loadingVariants, setLoadingVariants] = useState(false)
  const [savingVariantId, setSavingVariantId] = useState(null)
  const [creatingVariant, setCreatingVariant] = useState(false)

  const loadProducts = useCallback(async function loadProducts() {
    setLoadingProducts(true)
    try {
      const rows = await adminService.getAdminProducts({ size: 100 })
      setProducts(rows)
      setSelectedProduct((current) => {
        if (!current) return rows[0] || null
        return rows.find((product) => product.id === current.id) || rows[0] || null
      })
    } catch (error) {
      setNotice?.(`Chưa đọc được tồn kho: ${error.message}`)
    } finally {
      setLoadingProducts(false)
    }
  }, [setNotice])

  const loadVariants = useCallback(async function loadVariants(product, { silent = false } = {}) {
    if (!product) {
      setVariants([])
      return
    }

    setSelectedProduct(product)
    setLoadingVariants(true)
    try {
      const rows = await adminService.getAdminProductVariants(product.id)
      setVariants(rows)
      if (!silent) setNotice?.(`Đã tải ${rows.length} SKU của ${product.name}.`)
    } catch (error) {
      setNotice?.(`Chưa đọc được SKU: ${error.message}`)
    } finally {
      setLoadingVariants(false)
    }
  }, [setNotice])

  useEffect(() => {
    Promise.resolve().then(loadProducts)
  }, [loadProducts])

  useEffect(() => {
    if (selectedProduct) {
      Promise.resolve().then(() => loadVariants(selectedProduct, { silent: true }))
    }
  }, [loadVariants, selectedProduct])

  const filteredProducts = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return products
      .filter((product) => {
        const stock = Number(product.totalStock || 0)
        const matchesQuery = !normalizedQuery
          || product.name?.toLowerCase().includes(normalizedQuery)
          || product.brandName?.toLowerCase().includes(normalizedQuery)
          || product.categoryName?.toLowerCase().includes(normalizedQuery)
          || product.slug?.toLowerCase().includes(normalizedQuery)
          || String(product.id).includes(normalizedQuery)

        const matchesStock = stockFilter === 'ALL'
          || (stockFilter === 'LOW' && stock > 0 && stock <= threshold)
          || (stockFilter === 'OUT' && stock === 0)
          || (stockFilter === 'HEALTHY' && stock > threshold)

        return matchesQuery && matchesStock
      })
      .sort((a, b) => Number(a.totalStock || 0) - Number(b.totalStock || 0))
  }, [products, query, stockFilter, threshold])

  const summary = useMemo(() => {
    const totalStock = products.reduce((sum, product) => sum + Number(product.totalStock || 0), 0)
    const outOfStock = products.filter((product) => Number(product.totalStock || 0) === 0).length
    const lowStock = products.filter((product) => {
      const stock = Number(product.totalStock || 0)
      return stock > 0 && stock <= threshold
    }).length
    const healthy = products.filter((product) => Number(product.totalStock || 0) > threshold).length

    return {
      totalProducts: products.length,
      totalStock,
      outOfStock,
      lowStock,
      healthy,
    }
  }, [products, threshold])

  async function saveVariant(variant, patch) {
    const nextVariant = { ...variant, ...patch }
    const validationMessage = validateVariant(nextVariant)
    if (validationMessage) {
      setNotice?.(validationMessage)
      return
    }

    setVariants((rows) => rows.map((row) => (row.id === variant.id ? nextVariant : row)))
    setSavingVariantId(variant.id)

    try {
      await adminService.updateAdminProductVariant(variant.id, {
        stockQty: Number(nextVariant.stockQty || 0),
        priceAdjustment: Number(nextVariant.priceAdjustment || 0),
        isActive: Boolean(nextVariant.isActive),
      })
      setNotice?.(`Đã cập nhật SKU ${variant.sku}.`)
      await loadProducts()
    } catch (error) {
      setVariants((rows) => rows.map((row) => (row.id === variant.id ? variant : row)))
      setNotice?.(`Không cập nhật được SKU ${variant.sku}: ${error.message}`)
    } finally {
      setSavingVariantId(null)
    }
  }

  async function createVariant(event) {
    event.preventDefault()
    if (!selectedProduct) return

    const validationMessage = validateVariant(variantForm, { requireColorSize: true })
    if (validationMessage) {
      setNotice?.(validationMessage)
      return
    }

    setCreatingVariant(true)
    try {
      const created = await adminService.createAdminProductVariant(selectedProduct.id, {
        color: variantForm.color.trim(),
        size: variantForm.size.trim(),
        stockQty: Number(variantForm.stockQty || 0),
        priceAdjustment: Number(variantForm.priceAdjustment || 0),
      })
      setVariantForm(emptyVariantForm)
      setNotice?.(`Đã tạo SKU ${created.sku || `#${created.id}`}.`)
      await loadVariants(selectedProduct, { silent: true })
      await loadProducts()
    } catch (error) {
      setNotice?.(`Không tạo được biến thể tồn kho: ${error.message}`)
    } finally {
      setCreatingVariant(false)
    }
  }

  async function removeVariant(variant) {
    if (!window.confirm(`Xóa SKU "${variant.sku}"?`)) return
    try {
      await adminService.deleteAdminProductVariant(variant.id)
      setNotice?.(`Đã xóa SKU ${variant.sku}.`)
      await loadVariants(selectedProduct, { silent: true })
      await loadProducts()
    } catch (error) {
      setNotice?.(`Không xóa được SKU: ${error.message}`)
    }
  }

  return (
    <section className="inventory-page">
      <div className="inventory-hero">
        <div>
          <p className="eyebrow">Tồn kho</p>
          <h1>Quản lý tồn kho</h1>
          <p>Theo dõi sản phẩm sắp hết hàng, tạo SKU cho sản phẩm mới và chỉnh nhanh số lượng tồn.</p>
        </div>
        <button type="button" className="ghost" onClick={loadProducts} disabled={loadingProducts}>
          {loadingProducts ? 'Đang tải...' : 'Làm mới'}
        </button>
      </div>

      <div className="inventory-kpi-grid">
        <InventoryKpi label="Tổng tồn" value={summary.totalStock} />
        <InventoryKpi label="Sản phẩm" value={summary.totalProducts} />
        <InventoryKpi label="Sắp hết" value={summary.lowStock} tone="warning" />
        <InventoryKpi label="Hết hàng" value={summary.outOfStock} tone="danger" />
      </div>

      <div className="inventory-toolbar">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Tìm tên sản phẩm, thương hiệu, danh mục, slug hoặc ID..."
        />
        <label>
          Ngưỡng cảnh báo
          <input
            type="number"
            min="0"
            value={threshold}
            onChange={(event) => setThreshold(Number(event.target.value || 0))}
          />
        </label>
        <div className="inventory-segmented" aria-label="Lọc tồn kho">
          {stockFilters.map((filter) => (
            <button
              key={filter.value}
              type="button"
              className={stockFilter === filter.value ? 'active' : ''}
              onClick={() => setStockFilter(filter.value)}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      <div className="inventory-layout">
        <div className="inventory-product-panel">
          <div className="inventory-panel-heading">
            <div>
              <p className="eyebrow">Danh sách ưu tiên</p>
              <h2>{filteredProducts.length} sản phẩm</h2>
            </div>
            <span>{summary.healthy} đủ hàng</span>
          </div>

          {loadingProducts && <div className="admin-product-loading" />}
          {!loadingProducts && filteredProducts.length === 0 && <p className="empty-state">Chưa có sản phẩm phù hợp.</p>}

          <div className="inventory-product-list">
            {!loadingProducts && filteredProducts.map((product) => (
              <InventoryProductRow
                key={product.id}
                product={product}
                threshold={threshold}
                selected={selectedProduct?.id === product.id}
                onSelect={() => {
                  setVariantForm(emptyVariantForm)
                  loadVariants(product)
                }}
              />
            ))}
          </div>
        </div>

        <aside className="inventory-sku-panel">
          <div className="inventory-panel-heading">
            <div>
              <p className="eyebrow">SKU</p>
              <h2>{selectedProduct?.name || 'Chọn sản phẩm'}</h2>
            </div>
            {selectedProduct && <span>#{selectedProduct.id}</span>}
          </div>

          {selectedProduct && (
            <div className="inventory-product-summary">
              <span>{selectedProduct.brandName || 'Thương hiệu'}</span>
              <span>{selectedProduct.categoryName || 'Danh mục'}</span>
              <span>{Number(selectedProduct.totalStock || 0)} tồn kho</span>
            </div>
          )}

          {selectedProduct && (
            <form className="inventory-variant-create-form" onSubmit={createVariant}>
              <div className="inventory-panel-heading">
                <div>
                  <p className="eyebrow">Biến thể mới</p>
                  <h3>Tạo SKU tồn kho</h3>
                </div>
              </div>

              <div className="inventory-variant-create-grid">
                <label>Màu
                  <input
                    value={variantForm.color}
                    onChange={(event) => setVariantForm({ ...variantForm, color: event.target.value })}
                    placeholder="Đen, Trắng, Navy..."
                    required
                  />
                </label>
                <label>Size
                  <input
                    value={variantForm.size}
                    onChange={(event) => setVariantForm({ ...variantForm, size: event.target.value })}
                    placeholder="S, M, L, XL..."
                    required
                  />
                </label>
                <label>Số lượng tồn
                  <input
                    type="number"
                    min="0"
                    value={variantForm.stockQty}
                    onChange={(event) => setVariantForm({ ...variantForm, stockQty: event.target.value })}
                    required
                  />
                </label>
                <label>Giá cộng thêm
                  <input
                    type="number"
                    value={variantForm.priceAdjustment}
                    onChange={(event) => setVariantForm({ ...variantForm, priceAdjustment: event.target.value })}
                  />
                </label>
              </div>

              <button type="submit" disabled={creatingVariant}>
                {creatingVariant ? 'Đang tạo...' : 'Tạo biến thể'}
              </button>
            </form>
          )}

          {loadingVariants && <div className="inventory-sku-loading" />}
          {!loadingVariants && !selectedProduct && <p className="empty-state">Chọn một sản phẩm để xem, tạo và cập nhật SKU.</p>}
          {!loadingVariants && selectedProduct && variants.length === 0 && <p className="empty-state">Sản phẩm này chưa có SKU. Hãy tạo biến thể ở form phía trên.</p>}

          {!loadingVariants && variants.length > 0 && (
            <div className="inventory-sku-table">
              <div className="inventory-sku-head">
                <span>SKU</span>
                <span>Phân loại</span>
                <span>Tồn</span>
                <span>Giá cộng</span>
                <span>Bán</span>
              </div>

              {variants.map((variant) => (
                <VariantRow
                  key={`${variant.id}-${variant.stockQty}-${variant.priceAdjustment}-${variant.isActive}`}
                  variant={variant}
                  saving={savingVariantId === variant.id}
                  onSave={saveVariant}
                  onRemove={variants.length > 1 ? removeVariant : null}
                />
              ))}
            </div>
          )}
        </aside>
      </div>
    </section>
  )
}

function InventoryKpi({ label, value, tone = '' }) {
  return (
    <article className={`inventory-kpi ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function InventoryProductRow({ product, threshold, selected, onSelect }) {
  const stock = Number(product.totalStock || 0)
  const tone = stock === 0 ? 'danger' : stock <= threshold ? 'warning' : 'available'

  return (
    <button type="button" className={`inventory-product-row ${selected ? 'selected' : ''}`} onClick={onSelect}>
      <span className={`inventory-stock-dot ${tone}`} />
      <span>
        <strong>{product.name}</strong>
        <small>#{product.id} - {product.brandName || 'Thương hiệu'} - {product.categoryName || 'Danh mục'}</small>
      </span>
      <b>{stock}</b>
    </button>
  )
}

function VariantRow({ variant, saving, onSave, onRemove }) {
  const [draft, setDraft] = useState(() => toVariantDraft(variant))

  const dirty = Number(draft.stockQty) !== Number(variant.stockQty || 0)
    || Number(draft.priceAdjustment) !== Number(variant.priceAdjustment || 0)
    || Boolean(draft.isActive) !== Boolean(variant.isActive)

  const isDefault =
    String(variant.color || '').toUpperCase() === 'DEFAULT' &&
    String(variant.size || '').toUpperCase() === 'DEFAULT'

  return (
    <div className={`inventory-sku-row ${!variant.isActive ? 'inactive' : ''}`}>
      <div>
        <strong>{isDefault ? 'SKU mặc định' : variant.sku}</strong>
        <small>#{variant.id}</small>
      </div>
      <div>
        {isDefault ? (
          <span>Không có biến thể</span>
        ) : (
          <>
            <span>{variant.color || '-'}</span>
            <small>{variant.size || '-'}</small>
          </>
        )}
      </div>
      <input
        type="number"
        min="0"
        value={draft.stockQty}
        onChange={(event) => setDraft({ ...draft, stockQty: event.target.value })}
      />
      <input
        type="number"
        value={draft.priceAdjustment}
        onChange={(event) => setDraft({ ...draft, priceAdjustment: event.target.value })}
        aria-label={`Giá cộng ${variant.sku}`}
      />
      <div className="inventory-sku-actions">
        <label className="inventory-switch">
          <input
            type="checkbox"
            checked={draft.isActive}
            onChange={(event) => setDraft({ ...draft, isActive: event.target.checked })}
          />
          <span />
        </label>
        <button type="button" disabled={!dirty || saving} onClick={() => onSave(variant, draft)}>
          {saving ? 'Lưu...' : 'Lưu'}
        </button>
        {onRemove && (
          <button type="button" className="danger" onClick={() => onRemove(variant)} title="Xóa SKU">
            Xóa
          </button>
        )}
      </div>
      <small className="inventory-sku-price">{formatCurrency(Number(draft.priceAdjustment || 0))}</small>
    </div>
  )
}

function toVariantDraft(variant) {
  return {
    stockQty: Number(variant.stockQty || 0),
    priceAdjustment: Number(variant.priceAdjustment || 0),
    isActive: Boolean(variant.isActive),
  }
}

function validateVariant(variant, { requireColorSize = false } = {}) {
  if (requireColorSize && !variant.color?.trim()) return 'Vui lòng nhập màu cho biến thể.'
  if (requireColorSize && !variant.size?.trim()) return 'Vui lòng nhập size cho biến thể.'
  if (!Number.isFinite(Number(variant.stockQty)) || Number(variant.stockQty) < 0) return 'Số lượng tồn không hợp lệ.'
  if (!Number.isFinite(Number(variant.priceAdjustment || 0))) return 'Giá cộng thêm không hợp lệ.'
  return ''
}
