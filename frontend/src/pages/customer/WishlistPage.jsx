import { useEffect, useMemo, useState } from 'react'
import { getStoredAuth } from '../../services/api'
import * as productService from '../../services/productService'
import * as wishlistService from '../../services/wishlistService'
import { ROUTES } from '../../utils/constants'
import { formatCurrency } from '../../utils/formatCurrency'

export function WishlistPage({ setNotice, setRoute }) {
  const hasAuthToken = Boolean(getStoredAuth()?.token)
  const [items, setItems] = useState([])
  const [productMap, setProductMap] = useState({})
  const [loading, setLoading] = useState(hasAuthToken)
  const [removingProductId, setRemovingProductId] = useState(null)

  useEffect(() => {
    if (!hasAuthToken) return

    wishlistService.getWishlist()
      .then(setItems)
      .catch((error) => setNotice?.(`Chưa đọc được danh sách yêu thích: ${error.message}`))
      .finally(() => setLoading(false))
  }, [hasAuthToken, setNotice])

  useEffect(() => {
    async function loadProducts() {
      const ids = [...new Set(items.map((item) => item.productId).filter(Boolean))]
      if (!ids.length) {
        setProductMap({})
        return
      }

      const rows = await Promise.all(
        ids.map(async (id) => {
          try {
            return [id, await productService.getProduct(id)]
          } catch {
            return [id, null]
          }
        })
      )

      setProductMap(Object.fromEntries(rows))
    }

    loadProducts()
  }, [items])

  const summary = useMemo(() => {
    const availableCount = items.filter((item) => {
      const product = productMap[item.productId]
      const totalStock = product?.totalStock ?? product?.variants?.reduce((sum, variant) => sum + Number(variant.stockQty || 0), 0)
      return product ? Number(totalStock || 0) > 0 : false
    }).length

    return {
      total: items.length,
      availableCount,
      savedToday: items.filter((item) => isToday(item.createdAt)).length,
    }
  }, [items, productMap])

  async function remove(productIdToRemove) {
    setRemovingProductId(productIdToRemove)
    try {
      await wishlistService.removeFromWishlist(productIdToRemove)
      setItems(items.filter((item) => item.productId !== productIdToRemove))
      setNotice?.(`Đã xóa sản phẩm #${productIdToRemove} khỏi danh sách yêu thích.`)
    } catch (error) {
      setNotice?.(`Không xóa được khỏi danh sách yêu thích: ${error.message}`)
    } finally {
      setRemovingProductId(null)
    }
  }

  function viewProduct(item) {
    const product = productMap[item.productId]
    if (product) {
      sessionStorage.setItem('fashion:lastProductDetail', JSON.stringify(product))
    }
    setRoute?.(`${ROUTES.PRODUCT_DETAIL}?id=${encodeURIComponent(item.productId)}`)
  }

  return (
    <section className="wishlist-page">
      <aside className="wishlist-hero-panel">
        <div>
          <p className="eyebrow">Yêu thích</p>
          <h1>Bộ sưu tập của bạn</h1>
          <p>Lưu lại các sản phẩm đang cân nhắc để quay lại xem nhanh khi cần mua.</p>
        </div>

        <div className="wishlist-metric-grid">
          <div><span>Đã lưu</span><strong>{summary.total}</strong></div>
          <div><span>Còn hàng</span><strong>{summary.availableCount}</strong></div>
          <div><span>Hôm nay</span><strong>{summary.savedToday}</strong></div>
        </div>

        <p className="wishlist-save-hint">Nhấn biểu tượng tim trên sản phẩm để thêm hoặc bỏ yêu thích.</p>
      </aside>

      <div className="wishlist-content-panel">
        <div className="wishlist-content-heading">
          <div>
            <p className="eyebrow">Danh sách</p>
            <h2>Sản phẩm yêu thích</h2>
          </div>
          <span>{loading ? 'Đang tải...' : `${items.length} sản phẩm`}</span>
        </div>

        {!hasAuthToken && <p className="wishlist-empty-state">Vui lòng đăng nhập để xem danh sách yêu thích.</p>}
        {hasAuthToken && loading && <p className="wishlist-empty-state">Đang tải danh sách yêu thích...</p>}
        {hasAuthToken && !loading && items.length === 0 && <p className="wishlist-empty-state">Danh sách yêu thích đang trống.</p>}

        <div className="wishlist-card-grid">
          {!loading && items.map((item) => (
            <WishlistCard
              key={item.id || item.productId}
              item={item}
              product={productMap[item.productId]}
              removing={removingProductId === item.productId}
              onRemove={remove}
              onView={viewProduct}
            />
          ))}
        </div>
      </div>
    </section>
  )
}

function WishlistCard({ item, product, removing, onRemove, onView }) {
  const price = product?.salePrice || product?.basePrice
  const hasSale = product?.salePrice && product?.basePrice && product.salePrice < product.basePrice
  const totalStock = product?.totalStock ?? product?.variants?.reduce((sum, variant) => sum + Number(variant.stockQty || 0), 0)
  const inStock = product ? Number(totalStock || 0) > 0 : true

  return (
    <article className="wishlist-card">
      <div className="wishlist-product-image">
        {product?.primaryImageUrl ? (
          <img src={product.primaryImageUrl} alt={product.name} loading="lazy" />
        ) : (
          <span>#{item.productId}</span>
        )}
      </div>

      <div className="wishlist-card-body">
        <div>
          <p className="eyebrow">{product?.brandName || 'Sản phẩm yêu thích'}</p>
          <h2>{product?.name || `Sản phẩm #${item.productId}`}</h2>
          <p>{product?.material || 'Đang chờ tải thông tin sản phẩm'}</p>
        </div>

        <div className="wishlist-card-meta">
          <span className={product ? (inStock ? 'available' : 'soldout') : 'soldout'}>{product ? (inStock ? 'Còn hàng' : 'Hết hàng') : 'Sản phẩm hiện không còn bán'}</span>
          <span>Đã lưu {formatDate(item.createdAt)}</span>
        </div>

        <div className="wishlist-price-row">
          {price ? <strong>{formatCurrency(price)}</strong> : <strong>Đang cập nhật</strong>}
          {hasSale && <del>{formatCurrency(product.basePrice)}</del>}
        </div>

        <div className="wishlist-actions">
          <button type="button" className="ghost" onClick={() => onView(item)}>
            Xem chi tiết
          </button>
          {product && inStock && <button type="button" onClick={() => onView(item)}>
            Thêm vào giỏ
          </button>}
          <button type="button" className="danger" disabled={removing} onClick={() => onRemove(item.productId)}>
            {removing ? 'Đang xóa...' : 'Xóa'}
          </button>
        </div>
      </div>
    </article>
  )
}

function formatDate(value) {
  if (!value) return 'gần đây'
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value))
}

function isToday(value) {
  if (!value) return false
  const date = new Date(value)
  const today = new Date()
  return date.getFullYear() === today.getFullYear()
    && date.getMonth() === today.getMonth()
    && date.getDate() === today.getDate()
}
