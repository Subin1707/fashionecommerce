import { useEffect, useMemo, useState } from 'react'
import { useCart } from '../../hooks/useCart'
import { formatCurrency } from '../../utils/formatCurrency'
import { ROUTES } from '../../utils/constants'
import * as productService from '../../services/productService'
import './CartPage.css'

export function CartPage({ setNotice, setRoute }) {
  const {
    items,
    total,
    itemCount,
    cartLoading,
    loadCart,
    increaseQuantity,
    decreaseQuantity,
    removeItem,
  } = useCart()

  const [productMap, setProductMap] = useState({})
  const [updatingId, setUpdatingId] = useState(null)
  const [voucherInput, setVoucherInput] = useState('')
  const [appliedVoucher, setAppliedVoucher] = useState(null)
  const [voucherError, setVoucherError] = useState('')

  // Ngưỡng miễn phí vận chuyển: 500,000₫
  const FREE_SHIPPING_THRESHOLD = 500000
  const STANDARD_SHIPPING_FEE = 30000

  // ===============================
  // TÍNH TOÁN VẬN CHUYỂN & GIẢM GIÁ
  // ===============================
  const isFreeShipping = total >= FREE_SHIPPING_THRESHOLD || appliedVoucher?.type === 'FREESHIP'
  const shippingFee = isFreeShipping ? 0 : (total > 0 ? STANDARD_SHIPPING_FEE : 0)
  const shippingProgress = Math.min(100, (total / FREE_SHIPPING_THRESHOLD) * 100)
  const remainingForFreeShipping = Math.max(0, FREE_SHIPPING_THRESHOLD - total)

  const discountAmount = useMemo(() => {
    if (!appliedVoucher) return 0
    if (appliedVoucher.type === 'PERCENT') {
      return Math.round((total * appliedVoucher.value) / 100)
    }
    if (appliedVoucher.type === 'FIXED') {
      return Math.min(total, appliedVoucher.value)
    }
    return 0
  }, [appliedVoucher, total])

  const finalTotal = Math.max(0, total + shippingFee - discountAmount)

  // ===============================
  // LOAD CART
  // ===============================
  useEffect(() => {
    loadCart().catch(console.error)
  }, [loadCart])

  // ===============================
  // LOAD THÔNG TIN SẢN PHẨM (ẢNH, THƯƠNG HIỆU,...)
  // ===============================
  useEffect(() => {
    async function loadProducts() {
      const ids = [...new Set(items.map((item) => item.productId))]

      if (!ids.length) {
        setProductMap({})
        return
      }

      try {
        const results = await Promise.all(
          ids.map(async (id) => {
            try {
              const product = await productService.getProduct(id)
              return [id, product]
            } catch {
              return [id, null]
            }
          })
        )

        setProductMap(Object.fromEntries(results))
      } catch (error) {
        console.error('Không tải được thông tin sản phẩm:', error)
      }
    }

    loadProducts()
  }, [items])

  // ===============================
  // TĂNG / GIẢM SỐ LƯỢNG
  // ===============================
  async function handleIncrease(item) {
    try {
      setUpdatingId(item.id)
      await increaseQuantity(item)
    } catch (error) {
      setNotice?.(`Không thể tăng số lượng: ${error.message}`)
      alert(error.message)
    } finally {
      setUpdatingId(null)
    }
  }

  async function handleDecrease(item) {
    if (item.quantity <= 1) return
    try {
      setUpdatingId(item.id)
      await decreaseQuantity(item)
    } catch (error) {
      setNotice?.(`Không thể giảm số lượng: ${error.message}`)
      alert(error.message)
    } finally {
      setUpdatingId(null)
    }
  }

  // ===============================
  // XÓA 1 SẢN PHẨM
  // ===============================
  async function handleRemove(item) {
    const productName = productMap[item.productId]?.name || `Sản phẩm #${item.productId}`
    const confirmed = window.confirm(`Bạn có muốn bỏ "${productName}" (Size ${item.size || '-'}) khỏi giỏ hàng?`)
    if (!confirmed) return

    try {
      setUpdatingId(item.id)
      await removeItem(item.id)
      setNotice?.(`Đã xóa "${productName}" khỏi giỏ hàng.`)
    } catch (error) {
      setNotice?.(`Lỗi khi xóa: ${error.message}`)
      alert(error.message)
    } finally {
      setUpdatingId(null)
    }
  }

  // ===============================
  // XÓA TẤT CẢ SẢN PHẨM
  // ===============================
  async function handleClearAll() {
    if (!items.length) return
    const confirmed = window.confirm('Bạn có chắc chắn muốn xóa toàn bộ sản phẩm trong giỏ hàng?')
    if (!confirmed) return

    try {
      setUpdatingId('ALL')
      for (const item of items) {
        await removeItem(item.id)
      }
      setNotice?.('Đã làm trống giỏ hàng.')
    } catch (error) {
      setNotice?.(`Lỗi khi dọn giỏ hàng: ${error.message}`)
    } finally {
      setUpdatingId(null)
    }
  }

  // ===============================
  // ÁP DỤNG VOUCHER
  // ===============================
  function handleApplyVoucher(codeToApply) {
    const code = (codeToApply || voucherInput).trim().toUpperCase()
    setVoucherError('')

    if (!code) {
      setVoucherError('Vui lòng nhập mã ưu đãi.')
      return
    }

    if (code === 'FREESHIP') {
      setAppliedVoucher({ code: 'FREESHIP', type: 'FREESHIP', label: 'Miễn phí vận chuyển' })
      setNotice?.('Đã áp dụng mã miễn phí vận chuyển!')
      setVoucherInput('')
    } else if (code === 'FASHION10') {
      setAppliedVoucher({ code: 'FASHION10', type: 'PERCENT', value: 10, label: 'Giảm 10% đơn hàng' })
      setNotice?.('Đã áp dụng mã giảm giá 10%!')
      setVoucherInput('')
    } else if (code === 'VIP50K') {
      setAppliedVoucher({ code: 'VIP50K', type: 'FIXED', value: 50000, label: 'Giảm 50.000₫' })
      setNotice?.('Đã áp dụng giảm trực tiếp 50.000₫!')
      setVoucherInput('')
    } else {
      setVoucherError('Mã ưu đãi không hợp lệ hoặc đã hết hạn.')
    }
  }

  function handleRemoveVoucher() {
    setAppliedVoucher(null)
    setVoucherError('')
    setNotice?.('Đã hủy áp dụng mã giảm giá.')
  }

  // ===============================
  // ĐIỀU HƯỚNG
  // ===============================
  function goCheckout() {
    if (!items.length) return
    setRoute?.(ROUTES.CHECKOUT)
  }

  function continueShopping() {
    setRoute?.(ROUTES.PRODUCTS)
  }

  function goToProductDetail(productId) {
    setRoute?.(`${ROUTES.PRODUCT_DETAIL}?id=${productId}`)
  }

  // ===============================
  // RENDER: LOADING STATE
  // ===============================
  if (cartLoading && !items.length) {
    return (
      <div className="cart-container">
        <div className="cart-header-section">
          <span className="cart-eyebrow">Luxury Shopping Bag</span>
          <h1 className="cart-page-title">Đang tải giỏ hàng...</h1>
        </div>
        <div className="cart-skeleton-wrap">
          {[1, 2, 3].map((num) => (
            <div key={num} className="cart-skeleton-card">
              <div className="skeleton-shimmer skeleton-image" />
              <div className="skeleton-content">
                <div className="skeleton-shimmer skeleton-line" style={{ width: '35%' }} />
                <div className="skeleton-shimmer skeleton-line" style={{ width: '60%' }} />
                <div className="skeleton-shimmer skeleton-line" style={{ width: '20%' }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  // ===============================
  // RENDER: EMPTY STATE
  // ===============================
  if (!items.length) {
    return (
      <div className="cart-container">
        <section className="empty-cart-luxury">
          <div className="empty-cart-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
              <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z" />
              <path d="M3 6h18" />
              <path d="M16 10a4 4 0 0 1-8 0" />
            </svg>
          </div>

          <h2>Giỏ hàng của bạn đang trống</h2>
          <p>
            Bạn chưa chọn sản phẩm nào vào giỏ. Hãy dạo quanh bộ sưu tập thời trang của chúng tôi để tìm những mẫu thiết kế ưng ý nhất!
          </p>

          <button
            type="button"
            className="empty-cart-explore-btn"
            onClick={continueShopping}
          >
            Khám phá bộ sưu tập ngay
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          </button>

          <div className="empty-categories-quick">
            <div className="empty-quick-label">Danh mục gợi ý</div>
            <div className="quick-category-pills">
              {['Áo sơ mi', 'Đầm dự tiệc', 'Quần âu may đo', 'Bộ sưu tập Thu - Đông', 'Phụ kiện'].map((category) => (
                <button
                  key={category}
                  type="button"
                  className="quick-pill"
                  onClick={continueShopping}
                >
                  {category}
                </button>
              ))}
            </div>
          </div>
        </section>
      </div>
    )
  }

  // ===============================
  // RENDER: GIỎ HÀNG CÓ SẢN PHẨM
  // ===============================
  return (
    <div className="cart-container">
      {/* HEADER SECTION */}
      <section className="cart-header-section">
        <div className="cart-eyebrow">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3L12 3z" />
          </svg>
          Luxury Shopping Bag
        </div>

        <div className="cart-title-wrapper">
          <h1 className="cart-page-title">Giỏ hàng của bạn</h1>
          <span className="cart-count-badge">{itemCount} món đồ</span>
        </div>

        {/* FREE SHIPPING PROGRESS TRACKER */}
        <div className="cart-shipping-tracker">
          <div className="shipping-tracker-info">
            <div className={`tracker-status ${isFreeShipping ? 'unlocked' : ''}`}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <rect x="1" y="3" width="15" height="13" />
                <polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
                <circle cx="5.5" cy="18.5" r="2.5" />
                <circle cx="18.5" cy="18.5" r="2.5" />
              </svg>
              {isFreeShipping ? (
                <span>🎉 Chúc mừng! Đơn hàng của bạn đã được <strong>MIỄN PHÍ VẬN CHUYỂN</strong></span>
              ) : (
                <span>
                  Thêm <strong className="tracker-highlight">{formatCurrency(remainingForFreeShipping)}</strong> để được <strong>FREESHIP toàn quốc</strong>
                </span>
              )}
            </div>
            <span className="tracker-percent">{Math.round(shippingProgress)}%</span>
          </div>

          <div className="shipping-progress-track">
            <div
              className={`shipping-progress-fill ${isFreeShipping ? 'completed' : ''}`}
              style={{ width: `${shippingProgress}%` }}
            />
          </div>
        </div>
      </section>

      {/* 2-COLUMN LAYOUT */}
      <div className="cart-content-grid">
        {/* CỘT TRÁI: DANH SÁCH SẢN PHẨM */}
        <div className="cart-items-panel">
          <div className="cart-items-toolbar">
            <span className="toolbar-info">Danh sách sản phẩm ({items.length})</span>
            <button
              type="button"
              className="cart-clear-btn"
              disabled={updatingId != null}
              onClick={handleClearAll}
            >
              Dọn sạch giỏ hàng
            </button>
          </div>

          {items.map((item) => {
            const product = productMap[item.productId]
            const image = product?.primaryImageUrl || item.imageUrl
            const name = product?.name || item.productName || `Sản phẩm #${item.productId}`
            const brand = product?.brandName || 'Haute Couture'
            const lineTotal = Number(item.price || 0) * Number(item.quantity || 0)
            const isItemUpdating = updatingId === item.id || updatingId === 'ALL'

            return (
              <article
                key={item.id}
                className={`cart-item-card ${isItemUpdating ? 'is-updating' : ''}`}
              >
                {/* ẢNH SẢN PHẨM */}
                <div
                  className="item-image-box"
                  onClick={() => goToProductDetail(item.productId)}
                  title="Nhấn để xem chi tiết sản phẩm"
                >
                  {image ? (
                    <img src={image} alt={name} loading="lazy" />
                  ) : (
                    <div className="item-image-placeholder">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                        <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
                        <circle cx="9" cy="9" r="2" />
                        <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21" />
                      </svg>
                      <span>Thời trang</span>
                    </div>
                  )}
                </div>

                {/* THÔNG TIN CHI TIẾT */}
                <div className="item-info">
                  <span className="item-brand">{brand}</span>
                  <h2
                    className="item-title"
                    onClick={() => goToProductDetail(item.productId)}
                    title={name}
                  >
                    {name}
                  </h2>

                  <div className="item-meta-row">
                    <span className="item-variant-pill">
                      Size: <strong>{item.size || 'Tiêu chuẩn'}</strong>
                    </span>
                    <span className="item-stock-tag">Còn hàng</span>
                  </div>

                  <div className="item-unit-price">
                    Đơn giá: {formatCurrency(item.price)}
                  </div>
                </div>

                {/* BỘ ĐẾM SỐ LƯỢNG */}
                <div className="item-stepper-wrap">
                  <div className="luxury-stepper">
                    <button
                      type="button"
                      className="stepper-btn"
                      title="Giảm số lượng"
                      disabled={isItemUpdating || item.quantity <= 1}
                      onClick={() => handleDecrease(item)}
                    >
                      −
                    </button>
                    <span className="stepper-value">{item.quantity}</span>
                    <button
                      type="button"
                      className="stepper-btn"
                      title="Tăng số lượng"
                      disabled={isItemUpdating}
                      onClick={() => handleIncrease(item)}
                    >
                      +
                    </button>
                  </div>
                </div>

                {/* THÀNH TIỀN & NÚT XÓA */}
                <div className="item-price-col">
                  <span className="item-line-total">
                    {formatCurrency(lineTotal)}
                  </span>
                  <button
                    type="button"
                    className="item-remove-btn"
                    title="Xóa khỏi giỏ"
                    disabled={isItemUpdating}
                    onClick={() => handleRemove(item)}
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                      <path d="M3 6h18" />
                      <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
                      <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
                      <line x1="10" y1="11" x2="10" y2="17" />
                      <line x1="14" y1="11" x2="14" y2="17" />
                    </svg>
                  </button>
                </div>
              </article>
            )
          })}

          <div className="cart-items-footer">
            <button
              type="button"
              className="continue-shopping-link"
              onClick={continueShopping}
            >
              ← Tiếp tục xem thêm sản phẩm
            </button>
          </div>
        </div>

        {/* CỘT PHẢI: TÓM TẮT ĐƠN HÀNG (STICKY SUMMARY) */}
        <aside className="cart-summary-sidebar">
          <div className="summary-card">
            <h2 className="summary-title">Tóm tắt đơn hàng</h2>

            <div className="summary-rows">
              <div className="summary-row">
                <span>Tạm tính ({itemCount} món)</span>
                <strong>{formatCurrency(total)}</strong>
              </div>

              <div className="summary-row">
                <span>Phí vận chuyển</span>
                {isFreeShipping ? (
                  <span className="free-ship-tag">MIỄN PHÍ</span>
                ) : (
                  <strong>{formatCurrency(shippingFee)}</strong>
                )}
              </div>

              {appliedVoucher && (
                <div className="summary-row">
                  <span>Ưu đãi ({appliedVoucher.code})</span>
                  <strong style={{ color: '#1e7042' }}>-{formatCurrency(discountAmount)}</strong>
                </div>
              )}
            </div>

            {/* MÃ KHUYẾN MÃI */}
            <div className="summary-voucher-section">
              {appliedVoucher ? (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '13px' }}>
                  <div>
                    <span style={{ fontWeight: 600, color: '#1e7042' }}>✓ {appliedVoucher.label}</span>
                  </div>
                  <button
                    type="button"
                    style={{ background: 'none', border: 'none', color: '#c0392b', fontSize: '12px', cursor: 'pointer', textDecoration: 'underline' }}
                    onClick={handleRemoveVoucher}
                  >
                    Bỏ áp dụng
                  </button>
                </div>
              ) : (
                <>
                  <div className="voucher-input-group">
                    <input
                      type="text"
                      placeholder="Nhập mã voucher..."
                      value={voucherInput}
                      onChange={(e) => setVoucherInput(e.target.value)}
                      onKeyDown={(e) => e.key === 'Enter' && handleApplyVoucher()}
                    />
                    <button
                      type="button"
                      className="voucher-apply-btn"
                      onClick={() => handleApplyVoucher()}
                    >
                      Áp dụng
                    </button>
                  </div>
                  {voucherError && (
                    <div style={{ color: '#c0392b', fontSize: '11px', marginTop: '6px' }}>{voucherError}</div>
                  )}
                  <div className="voucher-hint-tags">
                    <span>Gợi ý:</span>
                    <span className="voucher-chip" onClick={() => handleApplyVoucher('FREESHIP')}>
                      FREESHIP
                    </span>
                    <span className="voucher-chip" onClick={() => handleApplyVoucher('FASHION10')}>
                      FASHION10 (-10%)
                    </span>
                  </div>
                </>
              )}
            </div>

            {/* TỔNG CỘNG */}
            <div className="summary-total-row">
              <span className="summary-total-label">Tổng thanh toán</span>
              <span className="summary-total-amount">{formatCurrency(finalTotal)}</span>
            </div>
            <p className="summary-vat-note">(Đã bao gồm thuế GTGT nếu có)</p>

            {/* NÚT THANH TOÁN CHÍNH */}
            <button
              type="button"
              className="checkout-cta-btn"
              onClick={goCheckout}
            >
              Tiến hành đặt hàng
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </button>

            {/* CAM KẾT VÀ BẢO ĐẢM */}
            <div className="summary-trust-badges">
              <div className="trust-item">
                <div className="trust-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  </svg>
                </div>
                <span>Cam kết 100% sản phẩm thiết kế chính hãng</span>
              </div>

              <div className="trust-item">
                <div className="trust-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                    <line x1="12" y1="22.08" x2="12" y2="12" />
                  </svg>
                </div>
                <span>Hỗ trợ đổi trả miễn phí trong 7 ngày</span>
              </div>

              <div className="trust-item">
                <div className="trust-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect width="20" height="14" x="2" y="5" rx="2" />
                    <line x1="2" y1="10" x2="22" y2="10" />
                  </svg>
                </div>
                <span>Thanh toán bảo mật chuẩn SSL / COD tận nơi</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}