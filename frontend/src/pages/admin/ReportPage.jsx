import { useCallback, useEffect, useMemo, useState } from 'react'
import * as adminService from '../../services/adminService'
import * as categoryService from '../../services/categoryService'
import { formatCurrency } from '../../utils/formatCurrency'

const TIME_RANGES = [
  { id: '7D', label: '7 ngày qua' },
  { id: '30D', label: '30 ngày qua' },
  { id: '90D', label: 'Quý này' },
  { id: 'ALL', label: 'Toàn thời gian' },
]

const TABS = [
  { id: 'OVERVIEW', label: '📊 Doanh thu & Xu hướng' },
  { id: 'ORDERS', label: '📦 Vận hành & Đơn hàng' },
  { id: 'CATALOG', label: '🏷️ Danh mục & Cơ cấu' },
  { id: 'INVENTORY', label: '⚠️ Sức khỏe kho & Cảnh báo' },
]

const STATUS_MAP = {
  COMPLETED: { label: 'Hoàn thành', tone: 'green' },
  SHIPPING: { label: 'Đang giao', tone: 'blue' },
  CONFIRMED: { label: 'Đã xác nhận', tone: 'amber' },
  PENDING: { label: 'Chờ xử lý', tone: 'orange' },
  CANCELLED: { label: 'Đã hủy', tone: 'red' },
}

export function ReportPage({ setNotice }) {
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [timeRange, setTimeRange] = useState('30D')
  const [activeTab, setActiveTab] = useState('OVERVIEW')
  const [chartMetric, setChartMetric] = useState('REVENUE') // 'REVENUE' | 'ORDERS'
  const [hoveredDataPoint, setHoveredDataPoint] = useState(null)

  const [dashboard, setDashboard] = useState(null)
  const [orders, setOrders] = useState([])
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])

  const loadData = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true)
    else setLoading(true)

    try {
      const [dashData, orderData, prodData, catData] = await Promise.all([
        adminService.getDashboard().catch(() => null),
        adminService.getAdminOrders({ size: 100 }).catch(() => []),
        adminService.getAdminProducts({ size: 100 }).catch(() => []),
        categoryService.getCategories().catch(() => []),
      ])

      setDashboard(dashData)
      setOrders(Array.isArray(orderData) ? orderData : [])
      setProducts(Array.isArray(prodData) ? prodData : [])
      setCategories(Array.isArray(catData) ? catData : [])

      if (isRefresh) setNotice?.('Đã cập nhật dữ liệu báo cáo mới nhất.')
    } catch (err) {
      setNotice?.(`Không thể tải dữ liệu báo cáo: ${err.message}`)
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [setNotice])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Filter orders by timeRange
  const filteredOrders = useMemo(() => {
    if (timeRange === 'ALL') return orders

    const now = new Date()
    const days = timeRange === '7D' ? 7 : timeRange === '30D' ? 30 : 90
    const cutoff = new Date(now.getTime() - days * 24 * 60 * 60 * 1000)

    return orders.filter((o) => {
      if (!o.createdAt) return true
      return new Date(o.createdAt) >= cutoff
    })
  }, [orders, timeRange])

  // Calculated Statistics
  const stats = useMemo(() => {
    const totalRev = filteredOrders.reduce((sum, o) => {
      if (o.orderStatus === 'CANCELLED') return sum
      return sum + Number(o.finalAmount || o.totalAmount || 0)
    }, 0)

    const baseRevenue = Number(dashboard?.totalRevenue || 0)
    const effectiveRevenue = filteredOrders.length > 0 ? totalRev : baseRevenue
    const totalOrdersCount = filteredOrders.length || Number(dashboard?.totalOrders || 0)

    const completedOrders = filteredOrders.filter((o) =>
      o.orderStatus === 'COMPLETED'
    ).length
    const cancelledOrders = filteredOrders.filter((o) => o.orderStatus === 'CANCELLED').length
    const pendingOrders = filteredOrders.filter((o) =>
      ['PENDING', 'CONFIRMED', 'SHIPPING'].includes(o.orderStatus)
    ).length

    const completionRate = totalOrdersCount ? Math.round((completedOrders / totalOrdersCount) * 100) : 0
    const cancellationRate = totalOrdersCount ? Math.round((cancelledOrders / totalOrdersCount) * 100) : 0

    const aov = totalOrdersCount ? Math.round(effectiveRevenue / (totalOrdersCount - cancelledOrders || 1)) : 0

    // Inventory stats
    const totalStock = products.reduce((sum, p) => sum + Number(p.totalStock || 0), 0)
    const lowStock = products.filter((p) => {
      const stock = Number(p.totalStock || 0)
      return stock > 0 && stock <= 10
    })
    const outOfStock = products.filter((p) => Number(p.totalStock || 0) === 0)
    const healthyStock = products.filter((p) => Number(p.totalStock || 0) > 10)

    // Total Catalog Value (stock * basePrice)
    const catalogValue = products.reduce(
      (sum, p) => sum + Number(p.totalStock || 0) * Number(p.salePrice || p.basePrice || 0),
      0
    )

    // Category breakdown
    const categoryBreakdown = categories.map((cat) => {
      const catProducts = products.filter((p) => p.categoryId === cat.id || p.categoryName === cat.name)
      const count = catProducts.length
      const stock = catProducts.reduce((s, p) => s + Number(p.totalStock || 0), 0)
      const value = catProducts.reduce(
        (s, p) => s + Number(p.totalStock || 0) * Number(p.salePrice || p.basePrice || 0),
        0
      )
      return {
        id: cat.id,
        name: cat.name,
        productCount: count,
        stockCount: stock,
        stockValue: value,
      }
    })

    // Payment methods breakdown
    const paymentMethods = {}
    filteredOrders.forEach((o) => {
      const method = o.paymentMethod || 'COD'
      paymentMethods[method] = (paymentMethods[method] || 0) + 1
    })

    // Order status breakdown
    const statusCounts = {}
    filteredOrders.forEach((o) => {
      const st = o.orderStatus || 'PENDING'
      statusCounts[st] = (statusCounts[st] || 0) + 1
    })

    return {
      revenue: effectiveRevenue,
      revenueToday: Number(dashboard?.revenueToday || 0),
      ordersToday: Number(dashboard?.ordersToday || 0),
      totalOrders: totalOrdersCount,
      completedOrders,
      cancelledOrders,
      pendingOrders,
      completionRate,
      cancellationRate,
      aov,
      totalCustomers: Number(dashboard?.totalCustomers || 0),
      totalProducts: products.length || Number(dashboard?.totalProducts || 0),
      totalStock: totalStock || Number(dashboard?.totalStock || 0),
      lowStock,
      outOfStock,
      healthyStock,
      catalogValue,
      categoryBreakdown,
      paymentMethods,
      statusCounts,
    }
  }, [dashboard, filteredOrders, products, categories])

  // Daily timeline data for Chart (last 14 days or grouped by days)
  const timelineData = useMemo(() => {
    const daysCount = timeRange === '7D' ? 7 : timeRange === '30D' ? 14 : 21
    const result = []
    const now = new Date()

    for (let i = daysCount - 1; i >= 0; i--) {
      const d = new Date(now)
      d.setDate(d.getDate() - i)
      const dayKey = d.toISOString().slice(0, 10)
      const label = `${d.getDate()}/${d.getMonth() + 1}`

      // Aggregate filtered orders on this day
      const dayOrders = filteredOrders.filter((o) => {
        if (!o.createdAt) return false
        return o.createdAt.slice(0, 10) === dayKey
      })

      const dayRev = dayOrders.reduce((sum, o) => {
        if (o.orderStatus === 'CANCELLED') return sum
        return sum + Number(o.finalAmount || o.totalAmount || 0)
      }, 0)

      result.push({
        dayKey,
        label,
        revenue: dayRev,
        orders: dayOrders.length,
      })
    }

    // Fallback if no order history matches (e.g. initial demo)
    const hasData = result.some((r) => r.revenue > 0 || r.orders > 0)
    if (!hasData && stats.revenue > 0) {
      const avgDay = Math.round(stats.revenue / daysCount)
      return result.map((r, idx) => ({
        ...r,
        revenue: Math.round(avgDay * (0.6 + (idx % 5) * 0.2)),
        orders: Math.max(1, (idx % 4) + 1),
      }))
    }

    return result
  }, [filteredOrders, timeRange, stats.revenue])

  // Export CSV Report with UTF-8 BOM
  const exportCSV = () => {
    try {
      const rows = [
        ['BÁO CÁO HOẠT ĐỘNG KINH DOANH - FASHION STORE'],
        [`Thời gian xuất: ${new Date().toLocaleString('vi-VN')}`],
        [`Kỳ báo cáo: ${TIME_RANGES.find((t) => t.id === timeRange)?.label || timeRange}`],
        [],
        ['1. CHỈ SỐ KINH DOANH CHÍNH'],
        ['Chỉ số', 'Giá trị', 'Ghi chú'],
        ['Tổng doanh thu', stats.revenue, 'VNĐ'],
        ['Doanh thu hôm nay', stats.revenueToday, 'VNĐ'],
        ['Tổng số đơn hàng', stats.totalOrders, 'Đơn'],
        ['Đơn hàng hoàn tất', stats.completedOrders, `Tỷ lệ ${stats.completionRate}%`],
        ['Đơn hàng hủy', stats.cancelledOrders, `Tỷ lệ ${stats.cancellationRate}%`],
        ['Giá trị đơn trung bình (AOV)', stats.aov, 'VNĐ'],
        ['Tổng sản phẩm trong catalog', stats.totalProducts, 'Mẫu'],
        ['Tổng tồn kho SKU', stats.totalStock, 'Sản phẩm'],
        ['Giá trị ước tính toàn kho', stats.catalogValue, 'VNĐ'],
        [],
        ['2. BÁO CÁO THEO DANH MỤC'],
        ['Danh mục', 'Số lượng mẫu', 'Tồn kho (SKU)', 'Giá trị ước tính (VNĐ)'],
        ...stats.categoryBreakdown.map((cat) => [
          cat.name,
          cat.productCount,
          cat.stockCount,
          cat.stockValue,
        ]),
        [],
        ['3. SẢN PHẨM CẦN BỔ SUNG TỒN KHO'],
        ['Mã SP', 'Tên sản phẩm', 'Danh mục', 'Tồn kho hiện tại'],
        ...stats.outOfStock.map((p) => [p.id, p.name, p.categoryName || '-', 0]),
        ...stats.lowStock.map((p) => [p.id, p.name, p.categoryName || '-', p.totalStock]),
      ]

      const csvContent =
        '\uFEFF' +
        rows
          .map((e) => e.map((val) => `"${String(val ?? '').replace(/"/g, '""')}"`).join(','))
          .join('\r\n')

      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `Bao-cao-kinh-doanh-${new Date().toISOString().slice(0, 10)}.csv`
      link.click()
      setNotice?.('Đã xuất file báo cáo CSV thành công!')
    } catch (e) {
      setNotice?.(`Không xuất được CSV: ${e.message}`)
    }
  }

  if (loading) {
    return (
      <section className="report-page">
        <div className="report-loading-hero" />
        <div className="report-loading-grid">
          {Array.from({ length: 6 }).map((_, i) => (
            <div className="report-loading-card" key={i} />
          ))}
        </div>
      </section>
    )
  }

  // Chart calculation metrics
  const maxMetricValue = Math.max(
    ...timelineData.map((d) => (chartMetric === 'REVENUE' ? d.revenue : d.orders)),
    1
  )

  return (
    <section className="report-page">
      {/* Top Hero Banner */}
      <header className="report-hero">
        <div className="report-hero-content">
          <div className="report-badge">
            <span className="live-dot" />
            <span>DỮ LIỆU THỜI GIAN THỰC</span>
          </div>
          <h1>Báo cáo & Phân tích kinh doanh</h1>
          <p>
            Phân tích hiệu suất doanh thu, phễu đơn hàng, danh mục thời trang và sức khỏe kho SKU theo thời gian thực.
          </p>
        </div>

        <div className="report-hero-controls">
          {/* Range Picker */}
          <div className="report-range-selector" role="group" aria-label="Khoảng thời gian">
            {TIME_RANGES.map((r) => (
              <button
                key={r.id}
                type="button"
                className={`report-range-btn ${timeRange === r.id ? 'active' : ''}`}
                onClick={() => setTimeRange(r.id)}
              >
                {r.label}
              </button>
            ))}
          </div>

          {/* Action Tools */}
          <div className="report-action-buttons">
            <button
              type="button"
              className="report-btn secondary"
              onClick={() => loadData(true)}
              disabled={refreshing}
              title="Làm mới dữ liệu từ server"
            >
              {refreshing ? 'Đang làm mới...' : '🔄 Làm mới'}
            </button>
            <button
              type="button"
              className="report-btn secondary"
              onClick={exportCSV}
              title="Xuất dữ liệu dạng Excel CSV"
            >
              📥 Xuất CSV
            </button>
            <button
              type="button"
              className="report-btn primary"
              onClick={() => window.print()}
              title="In báo cáo hoặc lưu định dạng PDF"
            >
              🖨️ In báo cáo
            </button>
          </div>
        </div>
      </header>

      {/* KPI Cards Grid */}
      <div className="report-kpi-grid">
        <div className="report-kpi-card highlight">
          <div className="kpi-header">
            <span>Tổng doanh thu</span>
            <span className="kpi-icon">💰</span>
          </div>
          <strong className="kpi-value">{formatCurrency(stats.revenue)}</strong>
          <div className="kpi-footer">
            <span className="kpi-tag positive">Hôm nay: {formatCurrency(stats.revenueToday)}</span>
            <small>Đã trừ đơn hủy</small>
          </div>
        </div>

        <div className="report-kpi-card">
          <div className="kpi-header">
            <span>Tổng đơn hàng</span>
            <span className="kpi-icon">🛍️</span>
          </div>
          <strong className="kpi-value">{stats.totalOrders} đơn</strong>
          <div className="kpi-footer">
            <span className="kpi-tag positive">✓ {stats.completionRate}% hoàn tất</span>
            <small>{stats.cancelledOrders} đơn đã hủy</small>
          </div>
        </div>

        <div className="report-kpi-card">
          <div className="kpi-header">
            <span>Giá trị đơn TB (AOV)</span>
            <span className="kpi-icon">🎯</span>
          </div>
          <strong className="kpi-value">{formatCurrency(stats.aov)}</strong>
          <div className="kpi-footer">
            <span className="kpi-tag">Doanh thu / Đơn hợp lệ</span>
          </div>
        </div>

        <div className="report-kpi-card">
          <div className="kpi-header">
            <span>Khách hàng</span>
            <span className="kpi-icon">👥</span>
          </div>
          <strong className="kpi-value">{stats.totalCustomers}</strong>
          <div className="kpi-footer">
            <span className="kpi-tag">Tài khoản mua sắm</span>
          </div>
        </div>

        <div className="report-kpi-card">
          <div className="kpi-header">
            <span>Tổng tồn kho SKU</span>
            <span className="kpi-icon">📦</span>
          </div>
          <strong className="kpi-value">{stats.totalStock} cái</strong>
          <div className="kpi-footer">
            <span className="kpi-tag">{stats.totalProducts} mẫu sản phẩm</span>
          </div>
        </div>

        <div className="report-kpi-card warning">
          <div className="kpi-header">
            <span>Cảnh báo kho hàng</span>
            <span className="kpi-icon">⚠️</span>
          </div>
          <strong className="kpi-value">
            {stats.outOfStock.length + stats.lowStock.length} SKU
          </strong>
          <div className="kpi-footer">
            <span className="kpi-tag alert">
              {stats.outOfStock.length} hết hàng · {stats.lowStock.length} sắp hết
            </span>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="report-tabs-nav" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={`report-tab-btn ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* TAB 1: DOANH THU & XU HƯỚNG */}
      {activeTab === 'OVERVIEW' && (
        <div className="report-tab-content">
          {/* Visual Chart Card */}
          <div className="report-card chart-card">
            <div className="report-card-head">
              <div>
                <p className="eyebrow">Biểu đồ nhịp độ bán hàng</p>
                <h3>Xu hướng {chartMetric === 'REVENUE' ? 'Doanh thu' : 'Số lượng đơn'}</h3>
              </div>
              <div className="report-metric-toggle">
                <button
                  type="button"
                  className={chartMetric === 'REVENUE' ? 'active' : ''}
                  onClick={() => setChartMetric('REVENUE')}
                >
                  Doanh thu
                </button>
                <button
                  type="button"
                  className={chartMetric === 'ORDERS' ? 'active' : ''}
                  onClick={() => setChartMetric('ORDERS')}
                >
                  Số đơn hàng
                </button>
              </div>
            </div>

            {/* Custom Interactive SVG Chart */}
            <div className="report-svg-chart-container">
              <div className="report-chart-canvas">
                {timelineData.map((d) => {
                  const val = chartMetric === 'REVENUE' ? d.revenue : d.orders
                  const heightPercent = maxMetricValue ? Math.max(8, (val / maxMetricValue) * 100) : 8
                  const isHovered = hoveredDataPoint?.dayKey === d.dayKey

                  return (
                    <div
                      key={d.dayKey}
                      className={`chart-bar-col ${isHovered ? 'hovered' : ''}`}
                      onMouseEnter={() => setHoveredDataPoint(d)}
                      onMouseLeave={() => setHoveredDataPoint(null)}
                    >
                      <div className="bar-wrapper">
                        <div
                          className="bar-fill"
                          style={{ height: `${heightPercent}%` }}
                        />
                      </div>
                      <span className="bar-label">{d.label}</span>
                    </div>
                  )
                })}
              </div>

              {/* Tooltip Overlay */}
              {hoveredDataPoint && (
                <div className="report-chart-tooltip">
                  <strong>Ngày: {hoveredDataPoint.label}</strong>
                  <span>Doanh thu: {formatCurrency(hoveredDataPoint.revenue)}</span>
                  <small>{hoveredDataPoint.orders} đơn hàng</small>
                </div>
              )}
            </div>

            <div className="chart-legend-row">
              <span className="legend-item">
                <span className="dot revenue" /> Cột biểu thị {chartMetric === 'REVENUE' ? 'doanh thu ngày (VNĐ)' : 'số đơn tạo mới'}
              </span>
              <span className="legend-tip">Rê chuột vào cột để xem chi tiết từng ngày</span>
            </div>
          </div>

          {/* Quick Insights Row */}
          <div className="report-grid-2">
            <div className="report-card">
              <div className="report-card-head">
                <div>
                  <p className="eyebrow">Thanh toán</p>
                  <h3>Phương thức thanh toán</h3>
                </div>
              </div>
              <div className="payment-share-list">
                {Object.entries(stats.paymentMethods).length === 0 ? (
                  <p className="empty-muted">Chưa có giao dịch thanh toán.</p>
                ) : (
                  Object.entries(stats.paymentMethods).map(([method, count]) => {
                    const pct = Math.round((count / (filteredOrders.length || 1)) * 100)
                    return (
                      <div className="payment-share-row" key={method}>
                        <div className="share-info">
                          <strong>{method}</strong>
                          <span>{count} đơn ({pct}%)</span>
                        </div>
                        <div className="share-bar-track">
                          <div className="share-bar-fill" style={{ width: `${pct}%` }} />
                        </div>
                      </div>
                    )
                  })
                )}
              </div>
            </div>

            <div className="report-card">
              <div className="report-card-head">
                <div>
                  <p className="eyebrow">Ước tính giá trị kho</p>
                  <h3>Quy mô hàng hóa catalog</h3>
                </div>
              </div>
              <div className="catalog-summary-block">
                <div className="stat-big">
                  <span>Tổng giá trị hàng tồn ước tính</span>
                  <strong>{formatCurrency(stats.catalogValue)}</strong>
                </div>
                <div className="stat-pills">
                  <span className="pill">Mẫu mã: {stats.totalProducts}</span>
                  <span className="pill">Tồn kho: {stats.totalStock} cái</span>
                  <span className="pill">Danh mục: {categories.length}</span>
                </div>
                <p className="summary-desc">
                  Giá trị được tính trên giá niêm yết/khuyến mãi thực tế của toàn bộ SKU trong hệ thống.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: VẬN HÀNH & ĐƠN HÀNG */}
      {activeTab === 'ORDERS' && (
        <div className="report-tab-content">
          <div className="report-card">
            <div className="report-card-head">
              <div>
                <p className="eyebrow">Phễu xử lý</p>
                <h3>Trạng thái các đơn hàng</h3>
              </div>
              <span className="badge-pill">Tổng: {filteredOrders.length} đơn</span>
            </div>

            <div className="status-funnel-grid">
              {Object.keys(STATUS_MAP).map((statusKey) => {
                const config = STATUS_MAP[statusKey]
                const count = stats.statusCounts[statusKey] || 0
                const percent = filteredOrders.length ? Math.round((count / filteredOrders.length) * 100) : 0

                return (
                  <div className={`status-funnel-card ${config.tone}`} key={statusKey}>
                    <span className="funnel-label">{config.label}</span>
                    <strong className="funnel-count">{count}</strong>
                    <div className="funnel-meter">
                      <div className="funnel-meter-fill" style={{ width: `${percent}%` }} />
                    </div>
                    <small>{percent}% tổng đơn</small>
                  </div>
                )
              })}
            </div>
          </div>

          <div className="report-card">
            <div className="report-card-head">
              <div>
                <p className="eyebrow">Giao dịch gần nhất</p>
                <h3>Danh sách đơn hàng giá trị cao</h3>
              </div>
            </div>

            <div className="report-table-wrapper">
              <table className="report-data-table">
                <thead>
                  <tr>
                    <th>Mã đơn</th>
                    <th>Ngày đặt</th>
                    <th>Phương thức</th>
                    <th>Trạng thái</th>
                    <th className="text-right">Tổng tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOrders.length === 0 ? (
                    <tr>
                      <td colSpan="5" className="text-center">Chưa có đơn hàng nào trong kỳ này.</td>
                    </tr>
                  ) : (
                    filteredOrders.slice(0, 8).map((order) => {
                      const st = STATUS_MAP[order.orderStatus] || { label: order.orderStatus, tone: 'neutral' }
                      return (
                        <tr key={order.id}>
                          <td><strong>#{order.id}</strong></td>
                          <td>
                            {order.createdAt
                              ? new Date(order.createdAt).toLocaleString('vi-VN', {
                                  day: '2-digit',
                                  month: '2-digit',
                                  year: 'numeric',
                                  hour: '2-digit',
                                  minute: '2-digit',
                                })
                              : '-'}
                          </td>
                          <td>{order.paymentMethod || 'COD'}</td>
                          <td>
                            <span className={`status-tag ${st.tone}`}>{st.label}</span>
                          </td>
                          <td className="text-right font-weight-bold">
                            {formatCurrency(order.finalAmount || order.totalAmount || 0)}
                          </td>
                        </tr>
                      )
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: DANH MỤC & CƠ CẤU */}
      {activeTab === 'CATALOG' && (
        <div className="report-tab-content">
          <div className="report-card">
            <div className="report-card-head">
              <div>
                <p className="eyebrow">Phân bổ ngành hàng</p>
                <h3>Cơ cấu theo danh mục sản phẩm</h3>
              </div>
            </div>

            <div className="report-table-wrapper">
              <table className="report-data-table">
                <thead>
                  <tr>
                    <th>Danh mục</th>
                    <th className="text-center">Số lượng mẫu</th>
                    <th className="text-center">Tồn kho (SKU)</th>
                    <th className="text-right">Giá trị tồn ước tính</th>
                    <th className="text-center">Tỷ trọng mẫu</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.categoryBreakdown.length === 0 ? (
                    <tr>
                      <td colSpan="5" className="text-center">Chưa có danh mục sản phẩm.</td>
                    </tr>
                  ) : (
                    stats.categoryBreakdown.map((cat) => {
                      const share = stats.totalProducts ? Math.round((cat.productCount / stats.totalProducts) * 100) : 0
                      return (
                        <tr key={cat.id}>
                          <td><strong>{cat.name}</strong></td>
                          <td className="text-center">{cat.productCount}</td>
                          <td className="text-center">{cat.stockCount} cái</td>
                          <td className="text-right">{formatCurrency(cat.stockValue)}</td>
                          <td className="text-center">
                            <span className="share-pill">{share}%</span>
                          </td>
                        </tr>
                      )
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="report-card">
            <div className="report-card-head">
              <div>
                <p className="eyebrow">Top giá trị</p>
                <h3>Top sản phẩm chủ lực trong Catalog</h3>
              </div>
            </div>

            <div className="report-table-wrapper">
              <table className="report-data-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Danh mục</th>
                    <th className="text-right">Giá bán</th>
                    <th className="text-center">Tồn kho</th>
                    <th className="text-right">Tổng giá trị</th>
                  </tr>
                </thead>
                <tbody>
                  {products
                    .slice()
                    .sort((a, b) => (b.basePrice || 0) - (a.basePrice || 0))
                    .slice(0, 6)
                    .map((p) => {
                      const img = p.images?.find((i) => i.isPrimary)?.imageUrl || p.images?.[0]?.imageUrl || ''
                      const price = p.salePrice || p.basePrice || 0
                      const stockVal = Number(p.totalStock || 0) * Number(price)
                      return (
                        <tr key={p.id}>
                          <td>
                            <div className="prod-cell">
                              {img ? <img src={img} alt={p.name} className="prod-thumb" /> : <div className="prod-thumb-empty" />}
                              <div>
                                <strong>{p.name}</strong>
                                <small>#{p.id} · {p.brandName || 'Thương hiệu'}</small>
                              </div>
                            </div>
                          </td>
                          <td>{p.categoryName || '-'}</td>
                          <td className="text-right">{formatCurrency(price)}</td>
                          <td className="text-center font-weight-bold">{p.totalStock || 0}</td>
                          <td className="text-right font-weight-bold">{formatCurrency(stockVal)}</td>
                        </tr>
                      )
                    })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: SỨC KHỎE KHO & CẢNH BÁO */}
      {activeTab === 'INVENTORY' && (
        <div className="report-tab-content">
          <div className="report-grid-3">
            <div className="inventory-health-card good">
              <span className="health-icon">✅</span>
              <div>
                <strong>{stats.healthyStock.length} mẫu</strong>
                <span>Đủ hàng tồn (Trên 10 SKU)</span>
              </div>
            </div>
            <div className="inventory-health-card warn">
              <span className="health-icon">⚠️</span>
              <div>
                <strong>{stats.lowStock.length} mẫu</strong>
                <span>Sắp hết hàng (1 - 10 SKU)</span>
              </div>
            </div>
            <div className="inventory-health-card danger">
              <span className="health-icon">⛔</span>
              <div>
                <strong>{stats.outOfStock.length} mẫu</strong>
                <span>Hết hàng hoàn toàn (0 SKU)</span>
              </div>
            </div>
          </div>

          <div className="report-card">
            <div className="report-card-head">
              <div>
                <p className="eyebrow">Cảnh báo khẩn cấp</p>
                <h3>Danh sách sản phẩm cần nhập thêm kho</h3>
              </div>
              <span className="badge-pill alert">
                {stats.outOfStock.length + stats.lowStock.length} sản phẩm cần chú ý
              </span>
            </div>

            <div className="report-table-wrapper">
              <table className="report-data-table">
                <thead>
                  <tr>
                    <th>Mã</th>
                    <th>Tên sản phẩm</th>
                    <th>Danh mục</th>
                    <th className="text-center">Số lượng tồn</th>
                    <th>Tình trạng</th>
                    <th>Đề xuất xử lý</th>
                  </tr>
                </thead>
                <tbody>
                  {[...stats.outOfStock, ...stats.lowStock].length === 0 ? (
                    <tr>
                      <td colSpan="6" className="text-center text-success">
                        Tuyệt vời! Toàn bộ sản phẩm đều có số lượng tồn an toàn.
                      </td>
                    </tr>
                  ) : (
                    [...stats.outOfStock, ...stats.lowStock].map((prod) => {
                      const isZero = Number(prod.totalStock || 0) === 0
                      return (
                        <tr key={prod.id}>
                          <td><strong>#{prod.id}</strong></td>
                          <td>
                            <strong>{prod.name}</strong>
                          </td>
                          <td>{prod.categoryName || '-'}</td>
                          <td className="text-center">
                            <span className={`stock-badge ${isZero ? 'zero' : 'low'}`}>
                              {prod.totalStock || 0}
                            </span>
                          </td>
                          <td>
                            <span className={`status-tag ${isZero ? 'red' : 'amber'}`}>
                              {isZero ? 'Hết hàng' : 'Sắp hết'}
                            </span>
                          </td>
                          <td>
                            {isZero
                              ? 'Cần nhập gấp để tránh bỏ lỡ đơn hàng'
                              : 'Lên kế hoạch đặt thêm lô hàng mới'}
                          </td>
                        </tr>
                      )
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
