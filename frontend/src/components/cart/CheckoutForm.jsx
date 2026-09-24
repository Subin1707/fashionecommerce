import { lazy, Suspense, useState } from 'react'
import { paymentMethods } from '../../services/paymentService'
import { formatCurrency } from '../../utils/formatCurrency'

const DeliveryLocationPicker = lazy(() => import('./DeliveryLocationPicker').then((module) => ({
  default: module.DeliveryLocationPicker,
})))

const paymentLabels = {
  COD: 'Thanh toán khi nhận hàng',
  BANK_TRANSFER: 'Chuyển khoản ngân hàng',
  E_WALLET: 'Ví điện tử',
  VNPAY: 'VNPay Sandbox',
}

const paymentNotes = {
  COD: 'Kiểm tra hàng trước khi thanh toán.',
  BANK_TRANSFER: 'Thông tin chuyển khoản sẽ được gửi sau khi đặt hàng.',
  E_WALLET: 'Phù hợp khi bạn muốn thanh toán nhanh.',
  VNPAY: 'Thanh toán qua cổng VNPay Sandbox.',
}

export function CheckoutForm({
  checkout,
  total,
  itemCount,
  items = [],
  buyNowItem,
  profileLoading,
  submitting,
  validationError,
  voucherMessage,
  voucherLoading,
  onChange,
  onApplyVoucher,
  onSubmit,
}) {
  const [showLocationPicker, setShowLocationPicker] = useState(() => checkout.shippingLat != null && checkout.shippingLng != null)
  const shippingFee = total > 0 ? Number(checkout.shippingFee || 0) : 0
  const discountAmount = Number(checkout.discountAmount || 0)
  const finalTotal = Math.max(0, Number(total || 0) + shippingFee - discountAmount)
  function updateField(field, value) {
    onChange({
      ...checkout,
      [field]: value,
      ...(field === 'voucherCode' ? { discountAmount: 0 } : {}),
    })
  }

  return (
    <form className="checkout-page" onSubmit={onSubmit}>
      <div className="checkout-main">
        <div className="checkout-heading">
          <p className="eyebrow">{buyNowItem ? 'Mua ngay' : 'Thanh toán'}</p>
          <h1>{buyNowItem ? 'Xác nhận mặt hàng đã chọn' : 'Hoàn tất đơn hàng'}</h1>
          <p>{profileLoading ? 'Đang lấy thông tin hồ sơ...' : 'Kiểm tra thông tin giao hàng trước khi đặt hàng.'}</p>
        </div>

        <section className="checkout-section">
          <div className="checkout-section-title">
            <span>1</span>
            <div>
              <h2>Người nhận hàng</h2>
              <p>Thông tin này sẽ được gắn vào địa chỉ giao hàng của đơn.</p>
            </div>
          </div>

          <div className="checkout-form-grid">
            <label>Họ và tên
              <input
                value={checkout.contactName}
                onChange={(event) => updateField('contactName', event.target.value)}
                placeholder="Nguyễn Văn A"
              />
            </label>

            <label>Số điện thoại
              <input
                value={checkout.contactPhone}
                onChange={(event) => updateField('contactPhone', event.target.value)}
                placeholder="09xxxxxxxx"
              />
            </label>

            <label className="checkout-wide-field">Địa chỉ giao hàng
              <textarea
                value={checkout.shippingAddress}
                onChange={(event) => onChange({ ...checkout, shippingAddress: event.target.value, shippingLat: null, shippingLng: null })}
                placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
              />
            </label>
            <label className="checkout-wide-field">Địa chỉ giao hàng chi tiết (nhập tay)
              <textarea
                name="shippingAddressDetail"
                value={checkout.shippingAddressDetail || ''}
                onChange={(event) => updateField('shippingAddressDetail', event.target.value)}
                placeholder="Ví dụ: Căn 1205, tầng 12, tòa CT4, cổng vào khu nhà"
                rows={3}
                maxLength={300}
                aria-describedby="shipping-address-detail-help"
              />
              <small id="shipping-address-detail-help">Bổ sung số nhà, căn hộ, tầng hoặc hướng dẫn tìm nhà. Thông tin này được lưu cùng địa chỉ trong đơn hàng.</small>
            </label>
            <div className="checkout-wide-field">
              {showLocationPicker ? (
                <Suspense fallback={<LocationPickerPlaceholder loading />}>
                  <DeliveryLocationPicker
                    latitude={checkout.shippingLat}
                    longitude={checkout.shippingLng}
                    address={checkout.shippingAddress}
                    onChange={(shippingLat, shippingLng, shippingAddress) => onChange({ ...checkout, shippingLat, shippingLng, shippingAddress })}
                  />
                </Suspense>
              ) : (
                <LocationPickerPlaceholder onOpen={() => setShowLocationPicker(true)} />
              )}
            </div>
          </div>
        </section>

        <section className="checkout-section">
          <div className="checkout-section-title">
            <span>2</span>
            <div>
              <h2>Phương thức thanh toán</h2>
              <p>Chọn cách thanh toán phù hợp với đơn hàng.</p>
            </div>
          </div>

          <div className="payment-method-grid">
            {paymentMethods.map((method) => (
              <label
                className={`payment-method-card ${checkout.paymentMethod === method ? 'selected' : ''}`}
                key={method}
              >
                <input
                  type="radio"
                  name="paymentMethod"
                  value={method}
                  checked={checkout.paymentMethod === method}
                  onChange={(event) => updateField('paymentMethod', event.target.value)}
                />
                <span>{paymentLabels[method] || method}</span>
                <small>{paymentNotes[method] || 'Xử lý sau khi đặt hàng.'}</small>
              </label>
            ))}
          </div>
        </section>

        <section className="checkout-section">
          <div className="checkout-section-title">
            <span>3</span>
            <div>
              <h2>Ưu đãi</h2>
              <p>Nhập mã voucher nếu bạn có.</p>
            </div>
          </div>

          <div className="voucher-row">
            <input
              value={checkout.voucherCode}
              onChange={(event) => updateField('voucherCode', event.target.value)}
              placeholder="Nhập mã giảm giá"
            />
            <button type="button" onClick={onApplyVoucher} disabled={voucherLoading}>
              {voucherLoading ? 'Đang kiểm tra...' : 'Áp dụng'}
            </button>
          </div>
          {voucherMessage && <p className="checkout-voucher-message" role="status">{voucherMessage}</p>}
        </section>
      </div>

      <div className="checkout-summary-panel">
        <div className="checkout-summary-heading">
          <div>
            <p className="eyebrow">Đơn hàng</p>
            <h2>{buyNowItem ? 'Sản phẩm mua ngay' : `${itemCount || 0} sản phẩm`}</h2>
          </div>
          <strong>{formatCurrency(finalTotal)}</strong>
        </div>

        {buyNowItem ? (
          <OrderItem
            imageUrl={buyNowItem.imageUrl}
            name={buyNowItem.productName}
            meta={`${buyNowItem.color || 'Màu mặc định'} / ${buyNowItem.size || 'Size chuẩn'}`}
            sku={buyNowItem.sku || buyNowItem.variantId}
            quantity={buyNowItem.quantity}
            price={buyNowItem.price}
          />
        ) : (
          <div className="checkout-item-list">
            {items.slice(0, 4).map((item) => (
              <OrderItem
                key={item.id}
                imageUrl={item.imageUrl}
                name={item.productName}
                meta={item.size || 'Size chuẩn'}
                sku={item.variantId}
                quantity={item.quantity}
                price={item.price}
              />
            ))}
            {items.length > 4 && <small className="checkout-more-items">+{items.length - 4} sản phẩm khác</small>}
            {!items.length && <p className="checkout-empty-note">Giỏ hàng đang trống.</p>}
          </div>
        )}

        <div className="checkout-price-box">
          <div><span>Tạm tính</span><strong>{formatCurrency(total)}</strong></div>
          <div><span>Phí vận chuyển</span><strong>{formatCurrency(shippingFee)}</strong></div>
          <div><span>Giảm giá</span><strong>-{formatCurrency(discountAmount)}</strong></div>
          <div className="checkout-grand-total"><span>Tổng thanh toán</span><strong>{formatCurrency(finalTotal)}</strong></div>
        </div>

        <button type="submit" className="checkout-submit-button" disabled={submitting}>
          {submitting ? 'Đang xử lý...' : buyNowItem ? 'Mua mặt hàng này' : 'Đặt hàng'}
        </button>
        {validationError && <p className="checkout-validation-error" role="alert">{validationError}</p>}
        <p className="checkout-safe-note">Đơn hàng sẽ được tạo sau khi hệ thống xác nhận voucher và tồn kho.</p>
      </div>
    </form>
  )
}

function LocationPickerPlaceholder({ loading = false, onOpen }) {
  return (
    <div className="delivery-location-placeholder">
      <div>
        <strong>Giao đến đúng nơi bạn muốn</strong>
        <p>Chọn điểm giao khi bạn đã sẵn sàng. Bản đồ sẽ chỉ tải sau thao tác này để trang thanh toán mở nhanh hơn.</p>
      </div>
      {loading ? (
        <span>Đang tải bản đồ...</span>
      ) : (
        <button type="button" onClick={onOpen}>Chọn điểm giao</button>
      )}
    </div>
  )
}

function OrderItem({ imageUrl, name, meta, sku, quantity, price }) {
  return (
    <div className="checkout-order-item">
      <div className="checkout-order-image">
        {imageUrl ? <img src={imageUrl} alt={name} /> : <span>SP</span>}
      </div>
      <div>
        <strong>{name || 'Sản phẩm'}</strong>
        <p>{meta}</p>
        <small>SKU: {sku || '-'}</small>
      </div>
      <div className="checkout-order-price">
        <span>x{quantity || 1}</span>
        <strong>{formatCurrency(Number(price || 0) * Number(quantity || 1))}</strong>
      </div>
    </div>
  )
}
