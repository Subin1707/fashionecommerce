import { useEffect, useMemo, useState } from 'react'
import { CheckoutForm } from '../../components/cart/CheckoutForm'
import { useCart } from '../../hooks/useCart'
import * as orderService from '../../services/orderService'
import * as paymentService from '../../services/paymentService'
import * as userService from '../../services/userService'
import * as voucherService from '../../services/voucherService'
import { ROUTES } from '../../utils/constants'

export function CheckoutPage({ setNotice, setRoute }) {
  const { items, total, itemCount, loadCart } = useCart()
  const buyNowItem = useMemo(() => readBuyNowItem(), [])
  const checkoutTotal = buyNowItem
    ? Number(buyNowItem.price || 0) * Number(buyNowItem.quantity || 1)
    : total
  const checkoutItemCount = buyNowItem ? Number(buyNowItem.quantity || 1) : itemCount
  const [checkout, setCheckout] = useState({
    contactName: '',
    contactPhone: '',
    shippingAddress: '',
    shippingAddressDetail: '',
    shippingLat: null,
    shippingLng: null,
    paymentMethod: 'COD',
    shippingFee: 30000,
    discountAmount: 0,
    voucherCode: '',
  })
  const [profileLoading, setProfileLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [validationError, setValidationError] = useState('')
  const [voucherMessage, setVoucherMessage] = useState('')
  const [voucherLoading, setVoucherLoading] = useState(false)

  function showValidationError(message) {
    setValidationError(message)
    setNotice?.(message)
  }

  useEffect(() => {
    let cancelled = false

    userService.getProfile()
      .then((profile) => {
        if (cancelled) return
        setCheckout((current) => ({
          ...current,
          contactName: current.contactName || profile?.fullName || '',
          contactPhone: current.contactPhone || profile?.phone || '',
          shippingAddress: current.shippingAddress || profile?.address || '',
        }))
      })
      .catch(() => {
        if (!cancelled) setNotice?.('Chưa lấy được thông tin hồ sơ để điền sẵn địa chỉ.')
      })
      .finally(() => {
        if (!cancelled) setProfileLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [setNotice])

  async function submitCheckout(event) {
    event.preventDefault()
    if (submitting) return
    if (!checkout.contactName.trim()) {
      showValidationError('Vui lòng nhập họ tên người nhận.')
      return
    }
    if (!checkout.contactPhone.trim()) {
      showValidationError('Vui lòng nhập số điện thoại người nhận.')
      return
    }
    if (!checkout.shippingAddress.trim()) {
      showValidationError('Vui lòng nhập địa chỉ giao hàng.')
      return
    }
    if (checkout.shippingLat == null || checkout.shippingLng == null) {
      showValidationError('Vui lòng chọn chính xác vị trí giao hàng trên bản đồ.')
      return
    }
    const shippingAddress = [
        checkout.contactName,
        checkout.contactPhone,
        checkout.shippingAddressDetail.trim(),
        checkout.shippingAddress,
      ].map((part) => part.trim()).filter(Boolean).join(' - ')
    if (shippingAddress.length > 500) {
      showValidationError('Thông tin người nhận và địa chỉ quá dài. Vui lòng rút gọn còn tối đa 500 ký tự.')
      return
    }
    try {
      setValidationError('')
      setSubmitting(true)

      const order = await orderService.checkout({
        paymentMethod: checkout.paymentMethod,
        shippingFee: checkout.shippingFee,
        discountAmount: checkout.discountAmount,
        voucherCode: checkout.voucherCode,
        shippingAddress,
        shippingLat: checkout.shippingLat,
        shippingLng: checkout.shippingLng,
        ...(buyNowItem ? {
          productId: buyNowItem.productId,
          variantId: buyNowItem.variantId,
          size: buyNowItem.size,
          quantity: buyNowItem.quantity,
        } : {}),
      })

      await loadCart()

      if (buyNowItem) {
        sessionStorage.removeItem('fashion:buyNowCheckout')
      }

      if (checkout.paymentMethod === 'VNPAY') {
        const payment = await paymentService.createVNPayPayment(order.id)
        window.location.assign(payment.paymentUrl)
        return
      }

      setNotice?.(`Đặt hàng #${order.id} thành công.`)
      setRoute?.(`${ROUTES.ORDER_DETAIL}?id=${order.id}`)
    } catch (error) {
      showValidationError(`Thanh toán chưa thành công: ${error.message}`)
    } finally {
      setSubmitting(false)
    }
  }

  async function applyVoucher() {
    const code = checkout.voucherCode.trim()
    if (!code) {
      setVoucherMessage('Vui lòng nhập mã giảm giá.')
      return
    }
    setVoucherLoading(true)
    setVoucherMessage('')
    try {
      const result = await voucherService.validateVoucher(code, checkoutTotal)
      if (!result.valid) {
        setCheckout((current) => ({ ...current, discountAmount: 0 }))
        setVoucherMessage(result.message || 'Mã giảm giá không hợp lệ.')
        return
      }
      setCheckout((current) => ({
        ...current,
        voucherCode: result.code || code.toUpperCase(),
        discountAmount: Number(result.discountAmount || 0),
      }))
      setVoucherMessage(result.message || 'Áp dụng mã giảm giá thành công.')
    } catch (error) {
      setCheckout((current) => ({ ...current, discountAmount: 0 }))
      setVoucherMessage(error.message || 'Không thể kiểm tra mã giảm giá.')
    } finally {
      setVoucherLoading(false)
    }
  }

  return (
    <CheckoutForm
      checkout={checkout}
      total={checkoutTotal}
      itemCount={checkoutItemCount}
      items={buyNowItem ? [] : items}
      buyNowItem={buyNowItem}
      profileLoading={profileLoading}
      submitting={submitting}
      validationError={validationError}
      voucherMessage={voucherMessage}
      voucherLoading={voucherLoading}
      onChange={(value) => { setCheckout(value); setValidationError('') }}
      onApplyVoucher={applyVoucher}
      onSubmit={submitCheckout}
    />
  )
}

function readBuyNowItem() {
  try {
    const raw = sessionStorage.getItem('fashion:buyNowCheckout')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}
