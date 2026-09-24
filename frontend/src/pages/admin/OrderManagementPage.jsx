import { ShipmentPanel } from '../../components/order/ShipmentPanel'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { ORDER_STATUSES } from '../../utils/constants'
import { formatCurrency } from '../../utils/formatCurrency'
import * as adminService from '../../services/adminService'

const statusLabels = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang chuẩn bị hàng',
  RETURNED: 'Đã hoàn hàng',
  SHIPPING: 'Đang giao',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
}

const paymentLabels = {
  PENDING: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  FAILED: 'Thất bại',
  REFUNDED: 'Đã hoàn tiền',
}

const paymentFilters = ['ALL', 'PENDING', 'PAID', 'FAILED', 'REFUNDED']

export function OrderManagementPage({ setNotice }) {
  const [orders, setOrders] = useState([])
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [paymentFilter, setPaymentFilter] = useState('ALL')
  const [amountFilter, setAmountFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [savingOrderId, setSavingOrderId] = useState(null)

  const loadOrders = useCallback(async function loadOrders() {
    setLoading(true)
    try {
      const rows = statusFilter === 'ALL'
        ? await adminService.getAdminOrders({ size: 100 })
        : await adminService.getAdminOrdersByStatus(statusFilter, { size: 100 })
      setOrders(rows)
      setSelectedOrder((current) => {
        if (!current) return rows[0] || null
        return rows.find((order) => order.id === current.id) || rows[0] || null
      })
    } catch (error) {
      setNotice?.(`Chưa đọc được đơn hàng quản trị: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }, [setNotice, statusFilter])

  useEffect(() => {
    Promise.resolve().then(loadOrders)
  }, [loadOrders])

  const filteredOrders = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return orders
      .filter((order) => {
        const amount = Number(order.finalAmount || 0)
        const matchesQuery = !normalizedQuery
          || String(order.id).includes(normalizedQuery)
          || String(order.userId).includes(normalizedQuery)
          || order.shippingAddress?.toLowerCase().includes(normalizedQuery)
          || order.paymentMethod?.toLowerCase().includes(normalizedQuery)

        const matchesPayment = paymentFilter === 'ALL' || order.paymentStatus === paymentFilter
        const matchesAmount = amountFilter === 'ALL'
          || (amountFilter === 'HIGH' && amount >= 1000000)
          || (amountFilter === 'MID' && amount >= 500000 && amount < 1000000)
          || (amountFilter === 'LOW' && amount < 500000)

        return matchesQuery && matchesPayment && matchesAmount
      })
      .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
  }, [orders, query, paymentFilter, amountFilter])

  const summary = useMemo(() => {
    const revenue = orders
      .filter((order) => !isCancelled(order))
      .reduce((sum, order) => sum + Number(order.finalAmount || 0), 0)
    const pending = orders.filter((order) => order.orderStatus === 'PENDING').length
    const shipping = orders.filter((order) => order.orderStatus === 'SHIPPING').length
    const completed = orders.filter((order) => order.orderStatus === 'COMPLETED').length

    return {
      total: orders.length,
      revenue,
      pending,
      shipping,
      completed,
    }
  }, [orders])

  async function updateStatus(order, orderStatus) {
    if (!order || order.orderStatus === orderStatus) return

    const previousOrder = order
    const nextOrder = { ...order, orderStatus }
    setSavingOrderId(order.id)
    setOrders((rows) => rows.map((row) => (row.id === order.id ? nextOrder : row)))
    setSelectedOrder((current) => (current?.id === order.id ? nextOrder : current))

    try {
      const updated = await adminService.updateAdminOrderStatus(order.id, orderStatus)
      setOrders((rows) => rows.map((row) => (row.id === order.id ? updated : row)))
      setSelectedOrder((current) => (current?.id === order.id ? updated : current))
      setNotice?.(`Đã cập nhật đơn hàng #${order.id} -> ${statusLabels[orderStatus] || orderStatus}.`)
    } catch (error) {
      setOrders((rows) => rows.map((row) => (row.id === order.id ? previousOrder : row)))
      setSelectedOrder((current) => (current?.id === order.id ? previousOrder : current))
      setNotice?.(`Không cập nhật được đơn hàng #${order.id}: ${error.message}`)
    } finally {
      setSavingOrderId(null)
    }
  }

  return (
    <section className="order-admin-page">
      <div className="order-admin-hero">
        <div>
          <p className="eyebrow">Đơn hàng</p>
          <h1>Quản lý đơn hàng</h1>
          <p>Theo dõi doanh thu, kiểm tra luồng xử lý và cập nhật trạng thái đơn ngay trong một màn hình.</p>
        </div>
        <button type="button" className="ghost" onClick={loadOrders} disabled={loading}>
          {loading ? 'Đang tải...' : 'Làm mới'}
        </button>
      </div>

      <div className="order-admin-kpi-grid">
        <OrderKpi label="Doanh thu" value={formatCurrency(summary.revenue)} />
        <OrderKpi label="Tổng đơn" value={summary.total} />
        <OrderKpi label="Chờ xử lý" value={summary.pending} tone="warning" />
        <OrderKpi label="Đang giao" value={summary.shipping} tone="info" />
      </div>

      <div className="order-admin-toolbar">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Tìm mã đơn, khách hàng, địa chỉ hoặc phương thức thanh toán..."
        />
        <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          <option value="ALL">Tất cả trạng thái</option>
          {ORDER_STATUSES.map((status) => (
            <option key={status} value={status}>{statusLabels[status] || status}</option>
          ))}
        </select>
        <select value={paymentFilter} onChange={(event) => setPaymentFilter(event.target.value)}>
          <option value="ALL">Tất cả thanh toán</option>
          {paymentFilters.filter((status) => status !== 'ALL').map((status) => (
            <option key={status} value={status}>{paymentLabels[status] || status}</option>
          ))}
        </select>
        <select value={amountFilter} onChange={(event) => setAmountFilter(event.target.value)}>
          <option value="ALL">Mọi giá trị</option>
          <option value="HIGH">Từ 1 triệu</option>
          <option value="MID">500k - 1 triệu</option>
          <option value="LOW">Dưới 500k</option>
        </select>
      </div>

      <div className="order-admin-layout">
        <div className="order-admin-list-panel">
          <div className="order-admin-panel-heading">
            <div>
              <p className="eyebrow">Hàng đợi</p>
              <h2>{filteredOrders.length} đơn hàng</h2>
            </div>
            <span>{summary.completed} đã giao/hoàn tất</span>
          </div>

          {loading && <div className="admin-product-loading" />}
          {!loading && filteredOrders.length === 0 && <p className="empty-state">Chưa có đơn hàng phù hợp.</p>}

          <div className="order-admin-list">
            {!loading && filteredOrders.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                selected={selectedOrder?.id === order.id}
                saving={savingOrderId === order.id}
                onSelect={() => setSelectedOrder(order)}
                onStatusChange={(status) => updateStatus(order, status)}
              />
            ))}
          </div>
        </div>

        <aside className="order-admin-detail-panel">
          <OrderDetail
            order={selectedOrder}
            saving={savingOrderId === selectedOrder?.id}
            onStatusChange={(status) => updateStatus(selectedOrder, status)}
          />
          {selectedOrder && <ShipmentPanel key={selectedOrder.id} order={selectedOrder} admin onUpdated={loadOrders} />}
        </aside>
      </div>
    </section>
  )
}

function OrderKpi({ label, value, tone = '' }) {
  return (
    <article className={`order-admin-kpi ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function OrderCard({ order, selected, saving, onSelect, onStatusChange }) {
  return (
    <article className={`order-admin-card ${selected ? 'selected' : ''}`}>
      <button type="button" className="order-admin-card-main" onClick={onSelect}>
        <span className={`order-status-dot ${statusTone(order.orderStatus)}`} />
        <span>
          <strong>Đơn #{order.id}</strong>
          <small>Khách #{order.userId || '-'} · {formatDateTime(order.createdAt)}</small>
        </span>
        <b>{formatCurrency(order.finalAmount || 0)}</b>
      </button>

      <div className="order-admin-card-meta">
        <span className={`order-admin-status ${statusTone(order.orderStatus)}`}>
          {statusLabels[order.orderStatus] || order.orderStatus || 'Chưa rõ'}
        </span>
        <span>{paymentLabels[order.paymentStatus] || order.paymentStatus || 'Thanh toán'}</span>
        <span>{order.paymentMethod || 'COD'}</span>
      </div>

      <div className="order-admin-card-actions">
        <select value={order.orderStatus || 'PENDING'} onChange={(event) => onStatusChange(event.target.value)} disabled={saving}>
          {allowedStatuses(order).map((status) => (
            <option key={status} value={status}>{statusLabels[status] || status}</option>
          ))}
        </select>
      </div>
    </article>
  )
}

function OrderDetail({ order, saving, onStatusChange }) {
  if (!order) {
    return (
      <div className="order-admin-empty-detail">
        <p className="eyebrow">Chi tiết</p>
        <h2>Chọn một đơn hàng</h2>
        <p>Thông tin giao hàng, thanh toán và luồng trạng thái sẽ hiển thị ở đây.</p>
      </div>
    )
  }

  return (
    <>
      <div className="order-admin-panel-heading">
        <div>
          <p className="eyebrow">Chi tiết</p>
          <h2>Đơn #{order.id}</h2>
        </div>
        <span className={`order-admin-status ${statusTone(order.orderStatus)}`}>
          {statusLabels[order.orderStatus] || order.orderStatus}
        </span>
      </div>

      <div className="order-admin-total-box">
        <span>Khách hàng #{order.userId || '-'}</span>
        <strong>{formatCurrency(order.finalAmount || 0)}</strong>
        <small>{formatDateTime(order.createdAt)}</small>
      </div>

      <div className="order-admin-flow">
        {allowedStatuses(order).filter((status) => status !== 'CANCELLED').map((status) => (
          <button
            key={status}
            type="button"
            className={order.orderStatus === status ? 'active' : ''}
            onClick={() => onStatusChange(status)}
            disabled={saving}
          >
            {status === 'SHIPPING' && order.orderStatus === 'PROCESSING' ? 'Bàn giao cho đơn vị vận chuyển' : statusLabels[status] || status}
          </button>
        ))}
      </div>

      <div className="order-admin-danger-row">
        <button type="button" className="danger" disabled={saving || order.orderStatus !== 'PENDING'} onClick={() => onStatusChange('CANCELLED')}>
          Hủy đơn
        </button>
      </div>

      <dl className="order-admin-detail-list">
        <div><dt>Tạm tính</dt><dd>{formatCurrency(order.totalAmount || 0)}</dd></div>
        <div><dt>Phí vận chuyển</dt><dd>{formatCurrency(order.shippingFee || 0)}</dd></div>
        <div><dt>Giảm giá</dt><dd>{formatCurrency(order.discountAmount || 0)}</dd></div>
        <div><dt>Thanh toán</dt><dd>{paymentLabels[order.paymentStatus] || order.paymentStatus || '-'}</dd></div>
        <div><dt>Phương thức</dt><dd>{order.paymentMethod || '-'}</dd></div>
        <div><dt>Cập nhật</dt><dd>{formatDateTime(order.updatedAt)}</dd></div>
      </dl>

      <div className="order-admin-address">
        <p className="eyebrow">Địa chỉ giao hàng</p>
        <p>{order.shippingAddress || 'Chưa có địa chỉ.'}</p>
      </div>
    </>
  )
}

function statusTone(status) {
  if (status === 'CANCELLED') return 'danger'
  if (status === 'PENDING') return 'warning'
  if (status === 'SHIPPING') return 'info'
  if (status === 'COMPLETED') return 'success'
  return 'neutral'
}

function isCancelled(order) {
  return order.orderStatus === 'CANCELLED'
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function allowedStatuses(order) {
  return [order.orderStatus, ...({ PENDING: ['CONFIRMED', 'CANCELLED'], CONFIRMED: ['PROCESSING'], PROCESSING: ['SHIPPING'] }[order.orderStatus] || [])]
}
