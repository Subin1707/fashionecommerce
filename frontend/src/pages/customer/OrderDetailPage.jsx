import { ShipmentPanel } from '../../components/order/ShipmentPanel'
import { useEffect, useState } from 'react'
import * as orderService from '../../services/orderService'
import * as reviewService from '../../services/reviewService'
import { ROUTES } from '../../utils/constants'
import { formatCurrency } from '../../utils/formatCurrency'
import { ReturnEvidence } from '../../components/order/ReturnEvidence'

const ORDER_STATUS_LABELS = { PENDING_PAYMENT: 'Chờ thanh toán', PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PROCESSING: 'Đang chuẩn bị hàng', RETURNED: 'Đã hoàn hàng', SHIPPING: 'Đang vận chuyển', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy' }
const PAYMENT_STATUS_LABELS = { PENDING: 'Chờ thanh toán', PAID: 'Đã thanh toán', COMPLETED: 'Đã thanh toán', FAILED: 'Thanh toán thất bại', REFUNDED: 'Đã hoàn tiền' }

export function OrderDetailPage({ setNotice, setRoute }) {
  const [order, setOrder] = useState(null)
  const [reviews, setReviews] = useState([])
  const [returnRequests, setReturnRequests] = useState([])
  const [returningItem, setReturningItem] = useState(null)
  const [returnType, setReturnType] = useState('RETURN')
  const [returnQuantity, setReturnQuantity] = useState(1)
  const [returnReason, setReturnReason] = useState('')
  const [returnFiles, setReturnFiles] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const id = new URLSearchParams(window.location.hash.split('?')[1] || '').get('id')

  useEffect(() => {
    let cancelled = false
    if (!id) return
    Promise.all([orderService.getOrder(id), reviewService.getMyReviews(), orderService.getReturnRequests()])
      .then(([data, rows, requests]) => { if (!cancelled) { setOrder(data); setReviews(rows); setReturnRequests(requests.filter((request) => String(request.orderId) === String(id))) } })
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

  async function submitReturnRequest(event) {
    event.preventDefault()
    if (!returningItem) return
    setBusy(true)
    try {
      const request = await orderService.createReturnRequest(order.id, {
        orderItemId: returningItem.id,
        requestType: returnType,
        quantity: Number(returnQuantity),
        reason: returnReason,
      }, returnFiles)
      setReturnRequests((current) => [request, ...current])
      setReturningItem(null)
      setReturnReason('')
      setReturnFiles([])
      setNotice?.('Đã gửi yêu cầu đổi trả. Shop sẽ kiểm tra và phản hồi sớm.')
    } catch (err) { setNotice?.(err.message) }
    finally { setBusy(false) }
  }

  if (!id || error) return <p role="alert">{error || 'Thiếu mã đơn hàng.'}</p>
  if (!order) return <p className="empty-state">Đang tải đơn hàng...</p>
  return <section className="table-panel">
    <button type="button" className="ghost" onClick={() => setRoute(ROUTES.ORDERS)}>Về đơn mua</button>
    <h2>Đơn hàng #{order.id}</h2>
    <p>{ORDER_STATUS_LABELS[order.orderStatus] || order.orderStatus} · {PAYMENT_STATUS_LABELS[order.paymentStatus] || order.paymentStatus} · Tạm tính: {formatCurrency(order.totalAmount)} · VAT: {formatCurrency(order.vatAmount || 0)} · Tổng tiền: {formatCurrency(order.finalAmount)}</p>
    <p>Giao đến: {order.shippingAddress}</p>
    <section className="return-request-section" aria-labelledby="return-request-heading">
      <h3 id="return-request-heading">Đổi / trả sản phẩm</h3>
      <p>Bạn có thể gửi yêu cầu trong vòng 14 ngày sau khi xác nhận đã nhận hàng.</p>
    </section>
    {order.orderStatus === 'SHIPPING' && <button type="button" disabled={busy} onClick={receive}>{busy ? 'Đang xác nhận...' : 'Đã nhận được hàng'}</button>}
    {order.orderStatus !== 'COMPLETED' && <p>Đánh giá được mở sau khi đơn hàng hoàn thành.</p>}
    <ShipmentPanel key={order.id} order={order} onUpdated={() => orderService.getOrder(order.id).then(setOrder).catch((err) => setNotice?.(err.message))} />
    {order.items.map((item) => {
      const reviewed = reviews.some((review) => String(review.orderItemId) === String(item.id))
      const returnRequest = returnRequests.find((request) => String(request.orderItemId) === String(item.id))
      return <article className="review-card" key={item.id}>
        <h3>{item.productName}</h3><p>{item.color} / {item.size} · Số lượng: {item.quantity} · Đơn giá: {formatCurrency(item.price)} · VAT dòng hàng: {formatCurrency(item.vatAmount || 0)}</p>
        {returnRequest ? <><p>Yêu cầu {returnRequest.requestType === 'EXCHANGE' ? 'đổi hàng' : 'trả hàng'}: {({ PENDING: 'Chờ shop xử lý', APPROVED: 'Đã duyệt, chờ gửi hàng', REJECTED: 'Từ chối', RECEIVED: 'Shop đã nhận hàng', COMPLETED: 'Đã hoàn tất' })[returnRequest.status] || returnRequest.status}{returnRequest.adminNote ? ` · ${returnRequest.adminNote}` : ''}</p><ReturnEvidence files={returnRequest.evidenceFiles} /></> : null}
        {!returnRequest && ['DELIVERED', 'COMPLETED'].includes(order.orderStatus) && <>
          {returningItem?.id === item.id ? <form onSubmit={submitReturnRequest} className="return-request-form">
            <label>Yêu cầu<select value={returnType} onChange={(event) => setReturnType(event.target.value)}><option value="RETURN">Trả hàng / hoàn tiền</option><option value="EXCHANGE">Đổi sản phẩm</option></select></label>
            <label>Số lượng<input type="number" min="1" max={item.quantity} value={returnQuantity} onChange={(event) => setReturnQuantity(event.target.value)} required /></label>
            <label>Lý do<textarea value={returnReason} onChange={(event) => setReturnReason(event.target.value)} maxLength="500" required /></label>
            <label>Ảnh / video minh chứng (1–5 tệp, tối đa 20 MB mỗi tệp)
              <input type="file" accept="image/jpeg,image/png,image/webp,video/mp4,video/webm" multiple required onChange={(event) => setReturnFiles(Array.from(event.target.files || []))} />
            </label>
            {!!returnFiles.length && <small>{returnFiles.map((file) => file.name).join(', ')}</small>}
            <button type="submit" disabled={busy || !returnReason.trim() || returnFiles.length === 0 || returnFiles.length > 5 || returnFiles.some((file) => file.size > 20 * 1024 * 1024)}>{busy ? 'Đang gửi...' : 'Gửi yêu cầu'}</button>
            <button type="button" className="ghost" onClick={() => setReturningItem(null)}>Đóng</button>
          </form> : <button type="button" className="ghost" onClick={() => { setReturningItem(item); setReturnQuantity(1) }}>Yêu cầu đổi / trả</button>}
        </>}
        {!returnRequest && !['DELIVERED', 'COMPLETED'].includes(order.orderStatus) && <div className="return-request-unavailable">
          <button type="button" className="ghost" disabled>Đổi / trả sau khi nhận hàng</button>
          <small>Hãy xác nhận đã nhận đơn để mở yêu cầu đổi trả cho sản phẩm này.</small>
        </div>}
        {order.orderStatus === 'COMPLETED' && (reviewed ? (
          <button type="button" className="ghost" onClick={() => setRoute(`${ROUTES.PRODUCT_DETAIL}?id=${item.productId}`)}>Xem đánh giá</button>
        ) : (
          <button type="button" onClick={() => setRoute(`${ROUTES.REVIEW}?orderItemId=${item.id}`)}>Đánh giá sản phẩm</button>
        ))}
      </article>
    })}
  </section>
}
