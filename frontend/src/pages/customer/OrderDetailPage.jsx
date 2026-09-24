import { ShipmentPanel } from '../../components/order/ShipmentPanel'
import { useEffect, useState } from 'react'
import * as orderService from '../../services/orderService'
import * as reviewService from '../../services/reviewService'
import { ROUTES } from '../../utils/constants'
import { formatCurrency } from '../../utils/formatCurrency'

const ORDER_STATUS_LABELS = { PENDING_PAYMENT: 'Chờ thanh toán', PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PROCESSING: 'Đang chuẩn bị hàng', RETURNED: 'Đã hoàn hàng', SHIPPING: 'Đang vận chuyển', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy' }
const PAYMENT_STATUS_LABELS = { PENDING: 'Chờ thanh toán', PAID: 'Đã thanh toán', COMPLETED: 'Đã thanh toán', FAILED: 'Thanh toán thất bại', REFUNDED: 'Đã hoàn tiền' }

export function OrderDetailPage({ setNotice, setRoute }) {
  const [order, setOrder] = useState(null)
  const [reviews, setReviews] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const id = new URLSearchParams(window.location.hash.split('?')[1] || '').get('id')

  useEffect(() => {
    let cancelled = false
    if (!id) return
    Promise.all([orderService.getOrder(id), reviewService.getMyReviews()])
      .then(([data, rows]) => { if (!cancelled) { setOrder(data); setReviews(rows) } })
      .catch((err) => { if (!cancelled) setError(err.message) })
    return () => { cancelled = true }
  }, [id])

  async function receive() {
    if (!window.confirm(`Bạn xác nhận đã nhận được đơn hàng #${order.id}?\n\nSau khi xác nhận, đơn hàng sẽ được hoàn thành.`)) return
    setBusy(true)
    try {
      setOrder(await orderService.confirmReceived(order.id))
      setNotice?.('Đã xác nhận nhận hàng. Bạn có thể đánh giá từng sản phẩm bên dưới.')
    } catch (err) { setNotice?.(err.message) }
    finally { setBusy(false) }
  }

  if (!id || error) return <p role="alert">{error || 'Thiếu mã đơn hàng.'}</p>
  if (!order) return <p className="empty-state">Đang tải đơn hàng...</p>
  return <section className="table-panel">
    <button type="button" className="ghost" onClick={() => setRoute(ROUTES.ORDERS)}>Về đơn mua</button>
    <h2>Đơn hàng #{order.id}</h2>
    <p>{ORDER_STATUS_LABELS[order.orderStatus] || order.orderStatus} · {PAYMENT_STATUS_LABELS[order.paymentStatus] || order.paymentStatus} · Tổng tiền: {formatCurrency(order.finalAmount)}</p>
    <p>Giao đến: {order.shippingAddress}</p>
    {order.orderStatus === 'SHIPPING' && <button type="button" disabled={busy} onClick={receive}>{busy ? 'Đang xác nhận...' : 'Đã nhận được hàng'}</button>}
    {order.orderStatus !== 'COMPLETED' && <p>Đánh giá được mở sau khi đơn hàng hoàn thành.</p>}
    <ShipmentPanel key={order.id} order={order} onUpdated={() => orderService.getOrder(order.id).then(setOrder).catch((err) => setNotice?.(err.message))} />
    {order.items.map((item) => {
      const reviewed = reviews.some((review) => String(review.orderItemId) === String(item.id))
      return <article className="review-card" key={item.id}>
        <h3>{item.productName}</h3><p>{item.color} / {item.size} · Số lượng: {item.quantity} · {formatCurrency(item.price)}</p>
        {order.orderStatus === 'COMPLETED' && (reviewed ? (
          <button type="button" className="ghost" onClick={() => setRoute(`${ROUTES.PRODUCT_DETAIL}?id=${item.productId}`)}>Xem đánh giá</button>
        ) : (
          <button type="button" onClick={() => setRoute(`${ROUTES.REVIEW}?orderItemId=${item.id}`)}>Đánh giá sản phẩm</button>
        ))}
      </article>
    })}
  </section>
}
