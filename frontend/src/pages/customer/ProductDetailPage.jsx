import { useEffect, useState } from 'react'
import { ProductDetail } from '../../components/product/ProductDetail'
import { SimilarProducts } from '../../components/product/SimilarProducts'
import { ReviewSummary } from '../../components/review/ReviewSummary'
import { useAuth } from '../../hooks/useAuth'
import { useCart } from '../../hooks/useCart'
import * as productService from '../../services/productService'
import * as wishlistService from '../../services/wishlistService'
import { ROUTES } from '../../utils/constants'

export function ProductDetailPage({ setNotice, setRoute }) {
  const detailId = new URLSearchParams(window.location.hash.split('?')[1] || '').get('id')
  const [product, setProduct] = useState(() => (detailId ? readCachedProduct(detailId) : null))
  const [loading, setLoading] = useState(() => Boolean(detailId))
  const [isAdding, setIsAdding] = useState(false)
  const [isBuying, setIsBuying] = useState(false)
  const [wishlistIds, setWishlistIds] = useState(new Set())
  const [isTogglingWishlist, setIsTogglingWishlist] = useState(false)
  const { isAdmin, isCustomer } = useAuth()
  const { addToCart } = useCart()

  useEffect(() => {
    if (!detailId) {
      setNotice?.('Thiếu mã sản phẩm.')
      return
    }

    const cachedProduct = readCachedProduct(detailId)
    productService.getProduct(detailId)
      .then((freshProduct) => {
        setProduct((currentProduct) => mergeProductImages(freshProduct, currentProduct || cachedProduct))
      })
      .catch((error) => setNotice?.(`Không đọc được sản phẩm: ${error.message}`))
      .finally(() => setLoading(false))
  }, [detailId, setNotice])

  useEffect(() => {
    if (!isCustomer || !detailId) {
      setWishlistIds(new Set())
      return
    }
    wishlistService.getWishlist()
      .then((items) => setWishlistIds(new Set(items.map((item) => String(item.productId)))))
      .catch(() => setWishlistIds(new Set()))
  }, [detailId, isCustomer])

  async function handleToggleWishlist(currentProduct) {
    if (!isCustomer) {
      sessionStorage.setItem('fashion:wishlist-return-route', window.location.hash.replace(/^#/, '') || `${ROUTES.PRODUCT_DETAIL}?id=${currentProduct.id}`)
      setNotice?.('Vui lòng đăng nhập bằng tài khoản Khách hàng để lưu sản phẩm yêu thích.')
      setRoute?.(ROUTES.LOGIN)
      return
    }

    setIsTogglingWishlist(true)
    try {
      const productKey = String(currentProduct.id)
      if (wishlistIds.has(productKey)) {
        await wishlistService.removeFromWishlist(currentProduct.id)
        setWishlistIds((current) => new Set([...current].filter((id) => id !== productKey)))
        setNotice?.(`Đã bỏ "${currentProduct.name}" khỏi danh sách yêu thích.`)
      } else {
        await wishlistService.addToWishlist(currentProduct.id)
        setWishlistIds((current) => new Set(current).add(productKey))
        setNotice?.(`Đã thêm "${currentProduct.name}" vào sản phẩm yêu thích.`)
      }
    } catch (error) {
      setNotice?.(`Không cập nhật được yêu thích: ${error.message}`)
    } finally {
      setIsTogglingWishlist(false)
    }
  }

  async function handleAdd(currentProduct, variant, quantity = 1) {
    if (isAdmin) {
      setNotice?.('Gio hang chi danh cho tai khoan Customer. Admin vui long dung trang quan tri de van hanh san pham.')
      return
    }

    if (!isCustomer) {
      setNotice?.('Vui lòng đăng nhập bằng tài khoản Khách hàng để thêm sản phẩm vào giỏ hàng.')
      setRoute?.(ROUTES.LOGIN)
      return
    }

    setIsAdding(true)
    try {
      await addToCart(currentProduct, variant, quantity)
      setNotice?.(`Đã thêm "${currentProduct.name}" vào giỏ hàng thành công!`)
    } catch (error) {
      setNotice?.(`Không thêm được sản phẩm vào giỏ hàng: ${error.message}`)
    } finally {
      setIsAdding(false)
    }
  }

  async function handleBuyNow(currentProduct, variant, quantity = 1) {
    if (isAdmin) {
      setNotice?.('Mua hang va gio hang chi danh cho tai khoan Customer. Admin vui long dang xuat neu muon dat hang thu.')
      return
    }

    if (!isCustomer) {
      setNotice?.('Vui lòng đăng nhập bằng tài khoản Khách hàng để mua sản phẩm.')
      setRoute?.(ROUTES.LOGIN)
      return
    }

    if (!variant?.id) {
      setNotice?.('Vui lòng chọn phân loại sản phẩm trước khi mua.')
      return
    }

    setIsBuying(true)
    try {
      const basePrice = Number(currentProduct.salePrice || currentProduct.basePrice || 0)
      const unitPrice = variant.priceAdjustment
        ? basePrice + Number(variant.priceAdjustment)
        : basePrice

      sessionStorage.setItem('fashion:buyNowCheckout', JSON.stringify({
        productId: Number(currentProduct.id),
        variantId: Number(variant.id),
        productName: currentProduct.name,
        imageUrl: currentProduct.primaryImageUrl || currentProduct.imageUrls?.[0] || '',
        color: variant.color,
        size: variant.size,
        sku: variant.sku,
        quantity: Number(quantity) || 1,
        price: unitPrice,
      }))

      setRoute?.(ROUTES.CHECKOUT)
    } finally {
      setIsBuying(false)
    }
  }

  if (loading) {
    return (
      <section className="product-detail-page">
        <div className="product-detail-loading">
          <div className="detail-loading-media" />
          <div className="detail-loading-copy">
            <span />
            <strong />
            <p />
            <p />
          </div>
        </div>
      </section>
    )
  }

  if (!product) return <p className="empty-state">Không tìm thấy sản phẩm.</p>

  function viewProduct(nextProduct) {
    sessionStorage.setItem('fashion:lastProductDetail', JSON.stringify(nextProduct))
    setRoute?.(`${ROUTES.PRODUCT_DETAIL}?id=${encodeURIComponent(nextProduct.id)}`)
  }

  return (
    <section className="product-detail-page">
      <ProductDetail
        key={product.id}
        product={product}
        onAdd={handleAdd}
        onBuyNow={handleBuyNow}
        isAdding={isAdding}
        isBuying={isBuying}
        onToggleWishlist={handleToggleWishlist}
        isWishlisted={wishlistIds.has(String(product.id))}
        isTogglingWishlist={isTogglingWishlist}
        onBack={() => setRoute?.(ROUTES.PRODUCTS)}
      />

      <div className="detail-lower-grid">
        <ReviewSummary product={product} />
        <div className="table-panel detail-care-panel">
          <h2>Chăm sóc sản phẩm</h2>
          <p>Giặt nhẹ với nước lạnh, phơi mặt trái và tránh sấy nhiệt cao để giữ form cùng màu vải.</p>
          <div className="detail-care-steps">
            <span>Giặt mặt trái</span>
            <span>Không tẩy mạnh</span>
            <span>Ủi nhiệt vừa</span>
          </div>
        </div>
      </div>
      <SimilarProducts
        productId={product.id}
        onAdd={handleAdd}
        onView={viewProduct}
        onToggleWishlist={handleToggleWishlist}
        isWishlisted={(productId) => wishlistIds.has(String(productId))}
        isTogglingWishlist={(productId) => isTogglingWishlist && String(productId) === String(product.id)}
      />
    </section>
  )
}

function readCachedProduct(id) {
  try {
    const raw = sessionStorage.getItem('fashion:lastProductDetail')
    if (!raw) return null
    const product = JSON.parse(raw)
    return String(product?.id) === String(id) ? product : null
  } catch {
    return null
  }
}

function mergeProductImages(freshProduct, fallbackProduct) {
  if (!fallbackProduct) return freshProduct

  const primaryImageUrl = freshProduct.primaryImageUrl || fallbackProduct.primaryImageUrl
  const imageUrls = freshProduct.imageUrls?.length
    ? freshProduct.imageUrls
    : fallbackProduct.imageUrls || (fallbackProduct.primaryImageUrl ? [fallbackProduct.primaryImageUrl] : [])

  return {
    ...freshProduct,
    primaryImageUrl,
    imageUrls,
  }
}
