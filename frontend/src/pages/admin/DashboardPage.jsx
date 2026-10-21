import { useEffect, useMemo, useState } from 'react'
import { formatCurrency } from '../../utils/formatCurrency'
import * as adminService from '../../services/adminService'
import { ROUTES } from '../../utils/constants'

export function DashboardPage({ setNotice, setRoute }) {
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [lastUpdated, setLastUpdated] = useState(null)

  useEffect(() => {
    let cancelled = false

    adminService.getDashboard()
      .then((data) => {
        if (!cancelled) {
          setDashboard(data)
          setLastUpdated(new Date())
        }
      })
      .catch((error) => setNotice?.(`Chưa đọc được bảng điều khiển: ${error.message}`))
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [setNotice])

  async function refreshDashboard() {
    setRefreshing(true)
    try {
      setDashboard(await adminService.getDashboard())
      setLastUpdated(new Date())
    } catch (error) {
      setNotice?.(`Chưa làm mới được bảng điều khiển: ${error.message}`)
    } finally {
      setRefreshing(false)
    }
  }

  const insights = useMemo(() => {
    const totalRevenue = Number(dashboard?.totalRevenue || 0)
    const revenueToday = Number(dashboard?.revenueToday || 0)
    const totalOrders = Number(dashboard?.totalOrders || 0)
    const totalProducts = Number(dashboard?.totalProducts || 0)
    const totalStock = Number(dashboard?.totalStock || 0)
    const averageOrderValue = Number(dashboard?.averageOrderValue || 0)

    return {
      revenueTodayRate: totalRevenue ? Math.min(100, Math.round((revenueToday / totalRevenue) * 100)) : 0,
      stockPerProduct: totalProducts ? Math.round(totalStock / totalProducts) : 0,
      orderDensity: totalProducts ? Math.round((totalOrders / totalProducts) * 10) / 10 : 0,
      averageOrderValue,
    }
  }, [dashboard])

  if (loading) {
    return (
      <section className="dashboard-page">
        <div className="dashboard-loading hero" />
        <div className="dashboard-loading-grid">
          {Array.from({ length: 6 }).map((_, index) => <div className="dashboard-loading" key={index} />)}
        </div>
      </section>
    )
  }

  return (
    <section className="dashboard-page">
      <div className="dashboard-page-heading">
        <div>
          <p className="eyebrow">BẢNG ĐIỀU KHIỂN</p>
          <h1>Tổng quan cửa hàng</h1>
          <p>Theo dõi doanh thu, đơn hàng và hàng hóa trong một nơi.</p>
        </div>
        <div className="dashboard-heading-actions">
          <span>{lastUpdated ? `Cập nhật ${lastUpdated.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}` : formatDate(dashboard?.dateTo)}</span>
          <button type="button" className="dashboard-refresh-button" onClick={refreshDashboard} disabled={refreshing}>
            <span aria-hidden="true">↻</span>{refreshing ? 'Đang cập nhật' : 'Làm mới'}
          </button>
        </div>
      </div>
      <div className="dashboard-hero-panel">
        <div>
          <p className="eyebrow">Tổng quan vận hành</p>
          <h1>Hiệu suất cửa hàng</h1>
          <p>
            Dữ liệu từ {formatDate(dashboard?.dateFrom)} đến {formatDate(dashboard?.dateTo)}, cập nhật theo trạng thái đơn hàng và tồn kho hiện tại.
          </p>
        </div>
        <div className="dashboard-hero-revenue">
          <span>Tổng doanh thu</span>
          <strong>{formatCurrency(dashboard?.totalRevenue)}</strong>
          <small>Hôm nay: {formatCurrency(dashboard?.revenueToday)}</small>
        </div>
      </div>

      <div className="dashboard-kpi-grid">
        <DashboardKpi icon="₫" label="Doanh thu hôm nay" value={formatCurrency(dashboard?.revenueToday)} note={`${dashboard?.ordersToday || 0} đơn hôm nay`} tone="warm" />
        <DashboardKpi icon="↗" label="Đơn hàng" value={dashboard?.totalOrders || 0} note={`Giá trị TB ${formatCurrency(insights.averageOrderValue)}`} />
        <DashboardKpi icon="▦" label="Sản phẩm" value={dashboard?.totalProducts || 0} note={`${insights.stockPerProduct} tồn kho/sản phẩm`} />
        <DashboardKpi icon="♙" label="Khách hàng" value={dashboard?.totalCustomers || 0} note="Tài khoản khách hàng" />
        <DashboardKpi icon="▤" label="Tồn kho" value={dashboard?.totalStock || 0} note="Tổng số lượng SKU" tone="green" />
        <DashboardKpi icon="◎" label="Mật độ đơn" value={insights.orderDensity} note="Đơn hàng trên mỗi sản phẩm" />
      </div>

      <div className="dashboard-content-grid">
        <div className="dashboard-panel">
          <div className="admin-section-heading">
            <div>
              <p className="eyebrow">Nhịp bán hàng</p>
              <h2>Doanh thu hôm nay so với kỳ xem</h2>
            </div>
            <span className="session-pill">{insights.revenueTodayRate}%</span>
          </div>
          <div className="dashboard-progress-track">
            <span style={{ width: `${insights.revenueTodayRate}%` }} />
          </div>
          <p className="dashboard-muted">
            Nếu tỷ lệ hôm nay thấp, hãy kiểm tra voucher, tồn kho nổi bật và các đơn đang chờ xử lý.
          </p>
        </div>

        <div className="dashboard-panel">
          <div className="admin-section-heading">
            <div>
              <p className="eyebrow">Cần chú ý</p>
              <h2>Tín hiệu vận hành</h2>
            </div>
          </div>
          <div className="dashboard-signal-list">
            <SignalItem label="Tồn kho trung bình" value={`${insights.stockPerProduct} sản phẩm/SKU`} />
            <SignalItem label="Đơn hôm nay" value={`${dashboard?.ordersToday || 0} đơn`} />
            <SignalItem label="Khách hàng" value={`${dashboard?.totalCustomers || 0} tài khoản`} />
          </div>
        </div>

        <div className="dashboard-panel dashboard-wide-panel">
          <div className="admin-section-heading">
            <div>
              <p className="eyebrow">Gợi ý thao tác</p>
              <h2>Việc nên kiểm tra tiếp theo</h2>
            </div>
          </div>
          <div className="dashboard-action-grid">
            <ActionCard title="Kiểm tra đơn mới" text="Mở danh sách đơn để theo dõi và cập nhật trạng thái giao hàng." route={ROUTES.ADMIN_ORDERS} setRoute={setRoute} />
            <ActionCard title="Rà tồn kho thấp" text="Kiểm tra số lượng SKU và bổ sung những mặt hàng sắp hết." route={ROUTES.ADMIN_INVENTORY} setRoute={setRoute} />
            <ActionCard title="Quản lý sản phẩm" text="Cập nhật hình ảnh, giá bán và sản phẩm nổi bật trên catalog." route={ROUTES.ADMIN_PRODUCTS} setRoute={setRoute} />
          </div>
        </div>
      </div>
    </section>
  )
}

function DashboardKpi({ icon, label, value, note, tone = '' }) {
  return (
    <article className={`dashboard-kpi-card ${tone}`}>
      <div className="dashboard-kpi-label"><span>{label}</span><i aria-hidden="true">{icon}</i></div>
      <strong>{value}</strong>
      <p>{note}</p>
    </article>
  )
}

function SignalItem({ label, value }) {
  return (
    <div className="dashboard-signal-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function ActionCard({ title, text, route, setRoute }) {
  return (
    <button type="button" className="dashboard-action-card" onClick={() => setRoute?.(route)}>
      <strong>{title}</strong>
      <p>{text}</p>
      <span className="dashboard-action-arrow" aria-hidden="true">→</span>
    </button>
  )
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value))
}
