import { useCallback, useEffect, useMemo, useState } from 'react'
import * as voucherService from '../../services/voucherService'
import { formatCurrency } from '../../utils/formatCurrency'

const emptyVoucher = {
  code: '',
  discountType: 'PERCENTAGE',
  discountValue: 10,
  minOrderValue: 200000,
  maxDiscount: 50000,
  startDate: new Date().toISOString().slice(0, 10),
  endDate: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10),
  status: 'ACTIVE',
}

export function DiscountManagementPage({ setNotice }) {
  const [vouchers, setVouchers] = useState([])
  const [editingVoucher, setEditingVoucher] = useState(null)
  const [activeTab, setActiveTab] = useState('FORM') // 'FORM' | 'SIMULATOR'
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [copiedCode, setCopiedCode] = useState(null)

  // Bộ giả lập kiểm tra voucher
  const [simulator, setSimulator] = useState({
    code: '',
    orderValue: 500000,
    loading: false,
    result: null,
    error: '',
  })

  const loadData = useCallback(async function loadData() {
    setLoading(true)
    try {
      const rows = await voucherService.getVouchers()
      setVouchers(Array.isArray(rows) ? rows : [])
    } catch (error) {
      setNotice?.(`Chưa đọc được danh sách ưu đãi: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }, [setNotice])

  useEffect(() => {
    Promise.resolve().then(loadData)
  }, [loadData])

  // KPIs
  const summary = useMemo(() => {
    const now = new Date()
    return {
      total: vouchers.length,
      active: vouchers.filter((v) => isVoucherActive(v, now)).length,
      inactive: vouchers.filter((v) => v.status === 'INACTIVE').length,
      expired: vouchers.filter((v) => isVoucherExpired(v, now)).length,
      percentage: vouchers.filter((v) => isPercentage(v)).length,
      fixed: vouchers.filter((v) => !isPercentage(v)).length,
    }
  }, [vouchers])

  // Lọc danh sách
  const filteredVouchers = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    const now = new Date()

    return vouchers
      .filter((v) => {
        const matchesQuery = !normalizedQuery
          || v.code?.toLowerCase().includes(normalizedQuery)
          || v.discountType?.toLowerCase().includes(normalizedQuery)
          || String(v.id).includes(normalizedQuery)

        let matchesStatus = true
        if (statusFilter === 'ACTIVE') matchesStatus = isVoucherActive(v, now)
        if (statusFilter === 'INACTIVE') matchesStatus = v.status === 'INACTIVE'
        if (statusFilter === 'EXPIRED') matchesStatus = isVoucherExpired(v, now)

        let matchesType = true
        if (typeFilter === 'PERCENTAGE') matchesType = isPercentage(v)
        if (typeFilter === 'FIXED_AMOUNT') matchesType = !isPercentage(v)

        return matchesQuery && matchesStatus && matchesType
      })
      .sort((a, b) => {
        const aActive = isVoucherActive(a, new Date())
        const bActive = isVoucherActive(b, new Date())
        return Number(bActive) - Number(aActive) || a.code.localeCompare(b.code)
      })
  }, [vouchers, query, statusFilter, typeFilter])

  function startCreate() {
    setEditingVoucher({ ...emptyVoucher })
    setActiveTab('FORM')
  }

  function startEdit(v) {
    setEditingVoucher({
      id: v.id,
      code: v.code || '',
      discountType: isPercentage(v) ? 'PERCENTAGE' : 'FIXED_AMOUNT',
      discountValue: Number(v.discountValue || 0),
      minOrderValue: Number(v.minOrderValue || 0),
      maxDiscount: Number(v.maxDiscount || 0),
      startDate: v.startDate ? v.startDate.slice(0, 10) : '',
      endDate: v.endDate ? v.endDate.slice(0, 10) : '',
      status: v.status || 'ACTIVE',
    })
    setActiveTab('FORM')
  }

  function updateDraft(patch) {
    setEditingVoucher((current) => ({ ...current, ...patch }))
  }

  function openSimulator(code) {
    setSimulator((prev) => ({
      ...prev,
      code: code || '',
      result: null,
      error: '',
    }))
    setActiveTab('SIMULATOR')
  }

  function copyCode(code) {
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(code)
      setCopiedCode(code)
      setTimeout(() => setCopiedCode(null), 2000)
    }
  }

  async function submitVoucher(event) {
    event.preventDefault()
    if (!editingVoucher) return

    setSaving(true)
    const payload = {
      ...editingVoucher,
      code: editingVoucher.code.trim().toUpperCase(),
      discountValue: Number(editingVoucher.discountValue),
      minOrderValue: Number(editingVoucher.minOrderValue || 0),
      maxDiscount: isPercentage(editingVoucher) ? Number(editingVoucher.maxDiscount || 0) : 0,
      startDate: editingVoucher.startDate ? `${editingVoucher.startDate}T00:00:00` : null,
      endDate: editingVoucher.endDate ? `${editingVoucher.endDate}T23:59:59` : null,
      status: editingVoucher.status,
    }

    try {
      let saved
      if (editingVoucher.id) {
        saved = await voucherService.updateVoucher(editingVoucher.id, payload)
        setVouchers((rows) => rows.map((row) => (row.id === saved.id ? saved : row)))
        setNotice?.(`Đã cập nhật mã ưu đãi "${saved.code}".`)
      } else {
        saved = await voucherService.createVoucher(payload)
        setVouchers((rows) => [saved, ...rows])
        setNotice?.(`Đã tạo thành công mã ưu đãi "${saved.code}".`)
      }

      setEditingVoucher(null)
      loadData()
    } catch (error) {
      setNotice?.(`Không lưu được mã giảm giá: ${error.message}`)
    } finally {
      setSaving(false)
    }
  }

  async function removeVoucher(v) {
    if (!window.confirm(`Bạn có chắc muốn xóa mã ưu đãi "${v.code}"?`)) return

    try {
      await voucherService.deleteVoucher(v.id)
      setVouchers((rows) => rows.filter((row) => row.id !== v.id))
      if (editingVoucher?.id === v.id) setEditingVoucher(null)
      setNotice?.(`Đã xóa mã ưu đãi "${v.code}".`)
      loadData()
    } catch (error) {
      setNotice?.(`Không xóa được mã giảm giá: ${error.message}`)
    }
  }

  async function runSimulation(event) {
    event.preventDefault()
    const code = simulator.code.trim().toUpperCase()
    if (!code) {
      setSimulator((prev) => ({ ...prev, error: 'Vui lòng nhập mã voucher cần kiểm tra.' }))
      return
    }

    setSimulator((prev) => ({ ...prev, loading: true, error: '', result: null }))

    try {
      const res = await voucherService.validateVoucher(code, Number(simulator.orderValue || 0))
      setSimulator((prev) => ({
        ...prev,
        result: res,
        loading: false,
      }))
    } catch (error) {
      setSimulator((prev) => ({
        ...prev,
        error: error.message || 'Mã ưu đãi không hợp lệ hoặc đã hết hạn.',
        loading: false,
      }))
    }
  }

  return (
    <section className="discount-admin-page">
      <div className="discount-admin-hero">
        <div>
          <p className="eyebrow">Chương trình ưu đãi</p>
          <h1>Quản lý mã giảm giá</h1>
          <p>Thiết lập chiến dịch khuyến mãi, mã voucher theo tỷ lệ % hoặc trừ tiền trực tiếp, kiểm soát giá trị đơn hàng tối thiểu và thời hạn áp dụng.</p>
        </div>
        <button type="button" onClick={startCreate}>+ Tạo mã giảm giá</button>
      </div>

      <div className="discount-admin-kpi-grid">
        <DiscountKpi label="Tổng mã ưu đãi" value={summary.total} />
        <DiscountKpi label="Đang hoạt động" value={summary.active} tone="success" />
        <DiscountKpi label="Tạm dừng" value={summary.inactive} tone="warning" />
        <DiscountKpi label="Đã hết hạn" value={summary.expired} />
        <DiscountKpi label="Giảm theo %" value={summary.percentage} />
        <DiscountKpi label="Giảm số tiền" value={summary.fixed} />
      </div>

      <div className="discount-admin-toolbar">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Tìm theo mã code, loại ưu đãi hoặc ID..."
        />
        <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          <option value="ALL">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang hoạt động</option>
          <option value="INACTIVE">Tạm dừng</option>
          <option value="EXPIRED">Đã hết hạn</option>
        </select>
        <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
          <option value="ALL">Tất cả hình thức</option>
          <option value="PERCENTAGE">Giảm theo phần trăm (%)</option>
          <option value="FIXED_AMOUNT">Giảm số tiền cố định (₫)</option>
        </select>
        <button type="button" className="ghost" onClick={loadData} disabled={loading}>
          {loading ? 'Đang tải...' : 'Làm mới'}
        </button>
      </div>

      <div className="discount-admin-layout">
        <div className="discount-admin-list-panel">
          <div className="discount-admin-panel-heading">
            <div>
              <p className="eyebrow">Danh sách mã</p>
              <h2>{filteredVouchers.length} mã ưu đãi</h2>
            </div>
            <span>Catalog khuyến mãi</span>
          </div>

          {loading && <div className="admin-product-loading" />}
          {!loading && filteredVouchers.length === 0 && (
            <p className="empty-state">Chưa có mã giảm giá nào phù hợp với bộ lọc.</p>
          )}

          <div className="discount-admin-grid">
            {!loading && filteredVouchers.map((v) => (
              <VoucherCard
                key={v.id || v.code}
                voucher={v}
                selected={editingVoucher?.id === v.id}
                isCopied={copiedCode === v.code}
                onCopy={() => copyCode(v.code)}
                onEdit={startEdit}
                onTest={() => openSimulator(v.code)}
                onRemove={removeVoucher}
              />
            ))}
          </div>
        </div>

        <aside className="discount-admin-editor-panel">
          <div className="discount-admin-tabs">
            <button
              type="button"
              className={`discount-admin-tab-btn ${activeTab === 'FORM' ? 'active' : ''}`}
              onClick={() => setActiveTab('FORM')}
            >
              Biểu mẫu Voucher
            </button>
            <button
              type="button"
              className={`discount-admin-tab-btn ${activeTab === 'SIMULATOR' ? 'active' : ''}`}
              onClick={() => setActiveTab('SIMULATOR')}
            >
              Giả lập kiểm tra
            </button>
          </div>

          {activeTab === 'FORM' ? (
            editingVoucher ? (
              <VoucherForm
                value={editingVoucher}
                saving={saving}
                onChange={updateDraft}
                onSubmit={submitVoucher}
                onCancel={() => setEditingVoucher(null)}
              />
            ) : (
              <div className="discount-admin-empty-editor">
                <p className="eyebrow">Thao tác</p>
                <h2>Chọn mã ưu đãi để chỉnh sửa</h2>
                <p>Hoặc tạo mới chương trình khuyến mại để kích cầu mua sắm và giữ chân khách hàng.</p>
                <button type="button" onClick={startCreate}>Tạo mã mới ngay</button>
              </div>
            )
          ) : (
            <VoucherSimulator
              state={simulator}
              onChange={(patch) => setSimulator((prev) => ({ ...prev, ...patch }))}
              onSubmit={runSimulation}
            />
          )}
        </aside>
      </div>
    </section>
  )
}

function DiscountKpi({ label, value, tone = '' }) {
  return (
    <article className={`discount-admin-kpi ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function VoucherCard({
  voucher,
  selected,
  isCopied,
  onCopy,
  onEdit,
  onTest,
  onRemove,
}) {
  const isPercent = isPercentage(voucher)
  const now = new Date()
  const isActive = isVoucherActive(voucher, now)
  const isExpired = isVoucherExpired(voucher, now)

  let badgeClass = 'active'
  let badgeLabel = 'Đang áp dụng'
  if (voucher.status === 'INACTIVE') {
    badgeClass = 'inactive'
    badgeLabel = 'Tạm dừng'
  } else if (isExpired) {
    badgeClass = 'expired'
    badgeLabel = 'Đã hết hạn'
  }

  return (
    <article className={`discount-admin-ticket ${selected ? 'selected' : ''} ${voucher.status === 'INACTIVE' ? 'inactive' : ''} ${isExpired ? 'expired' : ''}`}>
      <div className="discount-admin-ticket-head">
        <div className="discount-admin-code-wrap">
          <span className="discount-admin-code-pill">{voucher.code}</span>
          <button type="button" className="discount-admin-copy-btn" onClick={onCopy}>
            {isCopied ? 'Đã chép!' : 'Chép mã'}
          </button>
        </div>
        <span className={`discount-admin-badge ${badgeClass}`}>{badgeLabel}</span>
      </div>

      <div className="discount-admin-ticket-value">
        {isPercent ? `Giảm ${voucher.discountValue}%` : `Giảm ${formatCurrency(voucher.discountValue)}`}
        <span>{isPercent && Number(voucher.maxDiscount) > 0 ? `(Tối đa ${formatCurrency(voucher.maxDiscount)})` : ''}</span>
      </div>

      <div className="discount-admin-meta">
        <div>Đơn tối thiểu: <strong>{formatCurrency(voucher.minOrderValue || 0)}</strong></div>
        <div>
          Hạn dùng: <strong>{formatDateOnly(voucher.startDate)}</strong> → <strong>{formatDateOnly(voucher.endDate)}</strong>
        </div>
      </div>

      <div className="discount-admin-actions">
        <button type="button" className="ghost" onClick={onTest} title="Chạy thử nghiệm trên giá trị đơn hàng bất kỳ">
          Kiểm tra
        </button>
        <button type="button" onClick={() => onEdit(voucher)}>Sửa</button>
        <button type="button" className="danger" onClick={() => onRemove(voucher)}>Xóa</button>
      </div>
    </article>
  )
}

function VoucherForm({
  value,
  saving,
  onChange,
  onSubmit,
  onCancel,
}) {
  const isPercent = isPercentage(value)

  return (
    <form className="discount-admin-form" onSubmit={onSubmit}>
      <div className="discount-admin-panel-heading">
        <div>
          <p className="eyebrow">Biểu mẫu ưu đãi</p>
          <h2>{value.id ? `Sửa mã "${value.code}"` : 'Tạo mã ưu đãi mới'}</h2>
        </div>
        <span className={`discount-admin-badge ${value.status === 'ACTIVE' ? 'active' : 'inactive'}`}>
          {value.status === 'ACTIVE' ? 'Kích hoạt' : 'Tạm dừng'}
        </span>
      </div>

      {/* Live Ticket Preview */}
      <div className="discount-admin-preview">
        <span className="discount-admin-preview-label">Xem trước phiếu ưu đãi</span>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span className="discount-admin-code-pill">{value.code || 'MA-VOUCHER'}</span>
          <span className="discount-admin-badge active">{value.status === 'ACTIVE' ? 'Sẵn sàng' : 'Tạm dừng'}</span>
        </div>
        <div style={{ fontSize: '17px', fontWeight: 800 }}>
          {isPercent ? `Giảm ${value.discountValue || 0}%` : `Giảm ${formatCurrency(value.discountValue || 0)}`}
          {isPercent && Number(value.maxDiscount) > 0 && (
            <span style={{ fontSize: '12px', fontWeight: 500, color: '#6f6860', marginLeft: '6px' }}>
              (Tối đa {formatCurrency(value.maxDiscount)})
            </span>
          )}
        </div>
        <small style={{ color: '#6f6860' }}>
          Đơn từ {formatCurrency(value.minOrderValue || 0)} · Hạn: {formatDateOnly(value.startDate)} - {formatDateOnly(value.endDate)}
        </small>
      </div>

      <label>Mã ưu đãi (Code) *
        <input
          value={value.code}
          onChange={(event) => onChange({ code: event.target.value.toUpperCase().replace(/\s+/g, '') })}
          placeholder="VD: FASHION20, FREESHIP..."
          required
        />
      </label>

      <label>Loại giảm giá *
        <select
          value={value.discountType}
          onChange={(event) => onChange({ discountType: event.target.value })}
        >
          <option value="PERCENTAGE">Giảm theo phần trăm (%)</option>
          <option value="FIXED_AMOUNT">Giảm số tiền cố định (₫)</option>
        </select>
      </label>

      <label>
        {isPercent ? 'Mức giảm (%) *' : 'Số tiền giảm (₫) *'}
        <input
          type="number"
          min="1"
          max={isPercent ? 100 : 10000000}
          value={value.discountValue}
          onChange={(event) => onChange({ discountValue: event.target.value })}
          required
        />
      </label>

      <label>Giá trị đơn hàng tối thiểu (₫)
        <input
          type="number"
          min="0"
          step="10000"
          value={value.minOrderValue}
          onChange={(event) => onChange({ minOrderValue: event.target.value })}
          placeholder="VD: 200000"
        />
      </label>

      {isPercent && (
        <label>Mức giảm tối đa (₫) - 0 nếu không giới hạn
          <input
            type="number"
            min="0"
            step="10000"
            value={value.maxDiscount}
            onChange={(event) => onChange({ maxDiscount: event.target.value })}
            placeholder="VD: 50000"
          />
        </label>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
        <label>Ngày bắt đầu
          <input
            type="date"
            value={value.startDate}
            onChange={(event) => onChange({ startDate: event.target.value })}
          />
        </label>
        <label>Ngày kết thúc
          <input
            type="date"
            value={value.endDate}
            onChange={(event) => onChange({ endDate: event.target.value })}
          />
        </label>
      </div>

      <label>Trạng thái
        <select
          value={value.status}
          onChange={(event) => onChange({ status: event.target.value })}
        >
          <option value="ACTIVE">Kích hoạt (Hoạt động)</option>
          <option value="INACTIVE">Tạm dừng (Ẩn)</option>
        </select>
      </label>

      <div className="discount-admin-submit-row">
        <button type="button" className="ghost" onClick={onCancel} disabled={saving}>Hủy</button>
        <button type="submit" disabled={saving}>
          {saving ? 'Đang lưu...' : value.id ? 'Lưu thay đổi' : 'Tạo mã voucher'}
        </button>
      </div>
    </form>
  )
}

function VoucherSimulator({ state, onChange, onSubmit }) {
  return (
    <div className="discount-admin-simulator">
      <div className="discount-admin-panel-heading">
        <div>
          <p className="eyebrow">Công cụ kiểm thử</p>
          <h2>Giả lập áp dụng voucher</h2>
        </div>
      </div>

      <p style={{ color: '#6f6860', fontSize: '13.5px', lineHeight: 1.6 }}>
        Thử nghiệm xem mã voucher có áp dụng thành công trên giá trị đơn hàng thực tế của khách hay không và tính toán chính xác số tiền được chiết khấu.
      </p>

      <form className="discount-admin-simulator-form" onSubmit={onSubmit}>
        <label>Mã voucher thử nghiệm
          <input
            value={state.code}
            onChange={(event) => onChange({ code: event.target.value.toUpperCase().trim() })}
            placeholder="Nhập mã (VD: FASHION10, VIP50K)..."
            required
          />
        </label>

        <label>Giá trị đơn hàng giả lập (₫)
          <input
            type="number"
            min="0"
            step="10000"
            value={state.orderValue}
            onChange={(event) => onChange({ orderValue: event.target.value })}
            placeholder="VD: 500000"
            required
          />
        </label>

        <button type="submit" disabled={state.loading}>
          {state.loading ? 'Đang kiểm tra...' : 'Kiểm tra ngay'}
        </button>
      </form>

      {state.error && (
        <div className="discount-simulator-result error">
          <strong>❌ Không thể áp dụng mã</strong>
          <div>{state.error}</div>
        </div>
      )}

      {state.result && (
        <div className={`discount-simulator-result ${state.result.valid ? 'success' : 'error'}`}>
          <strong>{state.result.valid ? '✓ Áp dụng thành công!' : '❌ Không đủ điều kiện'}</strong>
          <div>{state.result.message}</div>
          {state.result.valid && (
            <div style={{ marginTop: '8px', paddingTop: '8px', borderTop: '1px dashed #a7cbb0' }}>
              <div>Đơn hàng ban đầu: <strong>{formatCurrency(state.orderValue)}</strong></div>
              <div>Số tiền giảm: <strong style={{ color: '#1e7042' }}>- {formatCurrency(state.result.discountAmount)}</strong></div>
              <div>Khách thanh toán: <strong style={{ color: '#9d533b', fontSize: '16px' }}>{formatCurrency(state.result.finalAmount)}</strong></div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function isPercentage(v) {
  const type = String(v?.discountType || '').toUpperCase()
  return type === 'PERCENTAGE' || type === 'PERCENT'
}

function isVoucherActive(v, now = new Date()) {
  if (v.status !== 'ACTIVE') return false
  if (v.endDate) {
    const end = new Date(v.endDate)
    if (end < now) return false
  }
  return true
}

function isVoucherExpired(v, now = new Date()) {
  if (v.status === 'EXPIRED') return true
  if (v.endDate) {
    const end = new Date(v.endDate)
    return end < now
  }
  return false
}

function formatDateOnly(isoString) {
  if (!isoString) return '--/--/----'
  try {
    const d = new Date(isoString)
    if (isNaN(d.getTime())) return String(isoString).slice(0, 10)
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
  } catch {
    return String(isoString).slice(0, 10)
  }
}
