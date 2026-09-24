import { useEffect, useRef, useState } from 'react'
import { apiRequest } from '../../services/api'
import { formatCurrency } from '../../utils/formatCurrency'
import { ShipmentMap } from './ShipmentMap'
import './ShipmentPanel.css'

const labels = {
  CREATED: 'Đã tạo vận đơn', WAITING_FOR_PICKUP: 'Chờ lấy hàng', PICKED_UP: 'Đã lấy hàng',
  IN_TRANSIT: 'Đang trung chuyển', OUT_FOR_DELIVERY: 'Đang giao tới khách', DELIVERED: 'Giao thành công',
  DELIVERY_FAILED: 'Giao thất bại', RETURNING: 'Đang hoàn hàng', RETURNED: 'Đã hoàn hàng',
}
const next = {
  CREATED: ['WAITING_FOR_PICKUP'], WAITING_FOR_PICKUP: ['PICKED_UP'], PICKED_UP: ['IN_TRANSIT'],
  IN_TRANSIT: ['OUT_FOR_DELIVERY'], OUT_FOR_DELIVERY: ['DELIVERED', 'DELIVERY_FAILED'],
  DELIVERY_FAILED: ['IN_TRANSIT', 'RETURNING'], RETURNING: ['RETURNED'],
}
const date = (value) => value ? new Date(value).toLocaleString('vi-VN') : ''
const activeStatuses = ['WAITING_FOR_PICKUP', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY']
const orderStages = ['PENDING_PAYMENT', 'PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'COMPLETED']
const stageLabels = ['Chờ thanh toán', 'Chờ xác nhận', 'Đã xác nhận', 'Chuẩn bị hàng', 'Đang vận chuyển', 'Hoàn thành']
const guidance = {
  PENDING_PAYMENT: ['Chờ hoàn tất thanh toán', 'Đơn hàng đang chờ thanh toán. Shop sẽ tiếp tục xử lý sau khi thanh toán được xác nhận.'],
  PENDING: ['Đã tiếp nhận đơn hàng', 'Shop đang chờ xác nhận đơn và kiểm tra sản phẩm. Mã vận đơn sẽ xuất hiện sau khi shop chuẩn bị hàng.'],
  CONFIRMED: ['Shop đã xác nhận đơn', 'Đơn hàng sẽ được chuẩn bị và đóng gói trước khi bàn giao cho đơn vị vận chuyển.'],
  PROCESSING: ['Đang chuẩn bị hàng', 'Shop đang chuẩn bị đơn để bàn giao. Thông tin đơn vị vận chuyển sẽ được cập nhật khi có vận đơn.'],
  SHIPPING: ['Đơn hàng đang được vận chuyển', 'Theo dõi các cập nhật giao hàng trong lịch sử bên dưới.'],
  COMPLETED: ['Đơn hàng đã hoàn thành', 'Bạn đã xác nhận nhận hàng. Bạn có thể đánh giá các sản phẩm trong đơn.'],
  CANCELLED: ['Đơn hàng đã hủy', 'Đơn hàng này không tiếp tục giao. Xem lịch sử bên dưới để kiểm tra cập nhật của shop.'],
  RETURNED: ['Đơn hàng đã hoàn về shop', 'Hành trình giao hàng đã kết thúc. Trạng thái thanh toán được hiển thị riêng trong thông tin đơn.'],
}
const shipmentGuidance = {
  CREATED: 'Vận đơn đã được tạo. Hàng chưa được xác nhận bàn giao cho đơn vị vận chuyển.',
  WAITING_FOR_PICKUP: 'Shop đang chờ đơn vị vận chuyển đến lấy hàng.',
  PICKED_UP: 'Đơn vị vận chuyển đã nhận hàng từ shop.',
  IN_TRANSIT: 'Đơn hàng đang trên đường đến khu vực nhận hàng.',
  OUT_FOR_DELIVERY: 'Đơn hàng đang được giao đến bạn. Vui lòng giữ liên lạc để nhận hàng.',
  DELIVERY_FAILED: 'Lần giao gần nhất chưa thành công. Xem lý do bên dưới và chờ cập nhật giao lại hoặc hoàn hàng.',
  RETURNING: 'Đơn hàng đang trên đường hoàn về shop.',
}

export function ShipmentPanel({ order, admin = false, onUpdated }) {
  const [data, setData] = useState(null)
  const [providers, setProviders] = useState([])
  const [providerId, setProviderId] = useState('')
  const [weightKg, setWeightKg] = useState('1')
  const [shippingFee, setShippingFee] = useState(String(order.shippingFee ?? 30000))
  const [estimatedDelivery, setEstimatedDelivery] = useState('')
  const [status, setStatus] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)
  const [loading, setLoading] = useState(true)
  const [lastUpdated, setLastUpdated] = useState(null)
  const updateRef = useRef(onUpdated)
  useEffect(() => { updateRef.current = onUpdated }, [onUpdated])
  const previousStatus = useRef(null)
  const url = `/api/${admin ? 'admin/orders' : 'orders'}/${order.id}/shipment`

  useEffect(() => {
    let active = true
    Promise.all([apiRequest(url), admin ? apiRequest('/api/admin/shipping-providers') : Promise.resolve([])])
      .then(([view, rows]) => {
        if (!active) return
        setData(view); setProviders(rows); setProviderId((current) => current || String(rows[0]?.id || ''))
        setStatus(next[view.shipment?.status]?.[0] || ''); setError('')
        setLastUpdated(new Date())
      })
      .catch((err) => { if (active) setError(err.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [url, admin, order.orderStatus, reload])

  useEffect(() => {
    const current = data?.shipment?.status
    if (previousStatus.current && current && previousStatus.current !== current) onUpdated?.()
    previousStatus.current = current
  }, [data?.shipment?.status, onUpdated])

  useEffect(() => {
    if (['CANCELLED', 'COMPLETED', 'RETURNED'].includes(order.orderStatus)) return undefined
    const timer = setInterval(() => {
      if (document.hidden) return
      setReload((value) => value + 1)
      updateRef.current?.()
    }, activeStatuses.includes(data?.shipment?.status) ? 5000 : 15000)
    return () => clearInterval(timer)
  }, [data?.shipment?.status, order.orderStatus])

  async function submit(event, create) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const view = await apiRequest(create ? url : `${url}/simulate`, {
        method: create ? 'POST' : 'PATCH',
        body: JSON.stringify(create
          ? { providerId: Number(providerId), weightKg: Number(weightKg), shippingFee: Number(shippingFee), estimatedDelivery: estimatedDelivery || null }
            : { status, description }),
          })
          setData(view); setStatus(next[view.shipment?.status]?.[0] || ''); setDescription('')
      onUpdated?.()
    } catch (err) { setError(err.message) }
    finally { setBusy(false) }
  }

  const shipment = data?.shipment
  const terminal = ['CANCELLED', 'RETURNED', 'COMPLETED'].includes(order.orderStatus)
  const [title, message] = guidance[order.orderStatus] || ['Đang cập nhật đơn hàng', 'Thông tin mới sẽ xuất hiện tại đây.']
  const visibleStages = orderStages.filter((stage) => stage !== 'PENDING_PAYMENT' || order.orderStatus === 'PENDING_PAYMENT' || data?.orderHistory?.some((entry) => entry.status === 'PENDING_PAYMENT'))
  const currentStage = visibleStages.indexOf(order.orderStatus)
  const exceptional = ['CANCELLED', 'RETURNED'].includes(order.orderStatus) || ['DELIVERY_FAILED', 'RETURNING', 'RETURNED'].includes(shipment?.status)
  const history = [...(data?.orderHistory || [])].reverse()
  const tracking = [...(data?.tracking || [])].reverse()

  return <section className="shipment-panel">
    <header className="shipment-heading">
      <div><span className="shipment-eyebrow">HÀNH TRÌNH ĐƠN HÀNG</span><h3>Theo dõi vận chuyển</h3></div>
      <button className="ghost" type="button" disabled={busy || loading} onClick={() => { setLoading(true); setReload((n) => n + 1); onUpdated?.() }}>{loading ? 'Đang cập nhật...' : 'Làm mới hành trình'}</button>
    </header>
    <div className={`shipment-status-card ${exceptional ? 'is-exception' : ''}`} aria-live="polite">
      <span className="shipment-status-symbol" aria-hidden="true">{exceptional ? '!' : terminal ? '✓' : '◷'}</span>
      <div><h4>{!terminal && shipment ? labels[shipment.status] || title : title}</h4>
        <p>{!terminal && shipment ? shipmentGuidance[shipment.status] || message : message}</p>
        {shipment?.status === 'DELIVERY_FAILED' && tracking[0]?.description && <p><strong>Lý do: </strong>{tracking[0].description}</p>}
      </div>
    </div>
    {!exceptional && <ol className="shipment-progress" aria-label="Tiến độ đơn hàng">
      {visibleStages.map((stage, index) => <li key={stage} className={index < currentStage ? 'is-done' : index === currentStage ? 'is-current' : ''} aria-current={index === currentStage ? 'step' : undefined}>
        <span aria-hidden="true">{index < currentStage ? '✓' : index + 1}</span><strong>{stageLabels[orderStages.indexOf(stage)]}</strong>
        <small>{index < currentStage ? 'Đã qua' : index === currentStage ? 'Hiện tại' : 'Chưa đến bước này'}</small>
      </li>)}
    </ol>}
    {error && <p className="shipment-error" role="alert">{error} {data ? 'Thông tin bên dưới là lần cập nhật thành công gần nhất.' : ''}</p>}
    {!data && !error && <p>Đang tải hành trình...</p>}
    <dl className="shipment-facts">
      <div><dt>Địa chỉ nhận hàng</dt><dd>{order.shippingAddress || 'Chưa có thông tin'}</dd></div>
      <div><dt>Đơn vị vận chuyển</dt><dd>{data?.provider?.name || 'Chưa được chỉ định'}</dd></div>
      <div><dt>Mã vận đơn</dt><dd>{shipment?.trackingCode || 'Chưa được cấp'}</dd></div>
      <div><dt>Giao dự kiến</dt><dd>{shipment?.estimatedDelivery ? new Date(`${shipment.estimatedDelivery}T00:00:00`).toLocaleDateString('vi-VN') : 'Chưa có lịch giao dự kiến'}</dd></div>
    </dl>
    {lastUpdated && <p className="shipment-sync">Cập nhật lúc {date(lastUpdated)}{!terminal && ' · Tự động cập nhật khi mở trang'}</p>}
    {data?.shipment && <>
      {admin && <p>Khối lượng: {data.shipment.weightKg} kg · Phí vận đơn: {formatCurrency(data.shipment.shippingFee)}</p>}
      <ShipmentMap shipment={data.shipment} route={data.route} />
    </>}
    {admin && data && !data.shipment && order.orderStatus === 'PROCESSING' && <form onSubmit={(event) => submit(event, true)}>
      <label>Đơn vị vận chuyển<select required value={providerId} onChange={(e) => setProviderId(e.target.value)}>
        <option value="">Chọn đơn vị</option>{providers.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
      </select></label>
      <label>Khối lượng (kg)<input type="number" min="0.01" step="0.01" required value={weightKg} onChange={(e) => setWeightKg(e.target.value)} /></label>
      <label>Phí vận chuyển (đ)<input type="number" min="0" step="1" required value={shippingFee} onChange={(e) => setShippingFee(e.target.value)} /></label>
      <label>Ngày giao dự kiến<input type="date" value={estimatedDelivery} onChange={(e) => setEstimatedDelivery(e.target.value)} /></label>
      <button disabled={busy || !providerId}>Tạo đơn vận chuyển</button>
    </form>}
    {admin && data?.shipment && next[data.shipment.status]?.length > 0 && <form onSubmit={(event) => submit(event, false)}>
      <h4>Bàn giao và cập nhật vận chuyển</h4>
      <p>Sau khi bàn giao lấy hàng, xe tự chạy theo tuyến và tự cập nhật trạng thái. Có thể cập nhật thủ công để thử giao thất bại hoặc hoàn hàng.</p>
      <label>Trạng thái<select value={status} onChange={(e) => setStatus(e.target.value)}>
        {next[data.shipment.status].map((s) => <option key={s} value={s}>{labels[s]}</option>)}
      </select></label>
      <label>{status === 'DELIVERY_FAILED' ? 'Lý do giao thất bại' : 'Mô tả'}<input maxLength={500} required={status === 'DELIVERY_FAILED'} value={description} onChange={(e) => setDescription(e.target.value)} /></label>
      <button disabled={busy || !status}>{data.shipment.status === 'WAITING_FOR_PICKUP' ? 'Bàn giao hàng' : 'Cập nhật vận chuyển'}</button>
    </form>}
    {!!history.length && <><h4 className="shipment-history-heading">Lịch sử đơn hàng <small>Mới nhất trước</small></h4><ol className="shipment-timeline">
      {history.map((entry, index) => <li key={entry.id} className={index === 0 ? 'is-latest' : ''}><time dateTime={entry.createdAt}>{date(entry.createdAt)}</time><div><strong>{entry.description}</strong>{index === 0 && <small>Cập nhật gần nhất</small>}</div></li>)}
    </ol></>}
    {!!data?.tracking?.length && <><h4>Hành trình giao hàng</h4><ol className="shipment-timeline">
      {tracking.map((entry, index) => <li key={entry.id} className={index === 0 ? 'is-latest' : ''}><time dateTime={entry.createdAt}>{date(entry.createdAt)}</time><div><strong>{labels[entry.status]}</strong><p>{entry.description}</p>
        {entry.location && <span>{entry.location}</span>}</div></li>)}
    </ol></>}
  </section>
}
