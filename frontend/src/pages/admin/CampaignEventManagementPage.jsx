import { useCallback, useEffect, useMemo, useState } from 'react'
import * as campaignEventService from '../../services/campaignEventService'
import { campaignImageSource } from '../../services/campaignEventService'
import './CampaignEventManagementPage.css'

const STATUS_OPTIONS = [
  ['DRAFT', 'Bản nháp'],
  ['SCHEDULED', 'Lên lịch'],
  ['ACTIVE', 'Xuất bản'],
  ['ENDED', 'Đã kết thúc'],
  ['HIDDEN', 'Đã ẩn'],
]

const STATUS_LABELS = {
  DRAFT: 'Bản nháp',
  SCHEDULED: 'Đã lên lịch',
  ACTIVE: 'Đang diễn ra',
  ENDED: 'Đã kết thúc',
  HIDDEN: 'Đã ẩn',
}

function localDateTime(value) {
  const date = value ? new Date(value) : new Date()
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

function createEmptyEvent() {
  const start = new Date()
  start.setSeconds(0, 0)
  const end = new Date(start.getTime() + 7 * 86400000)
  return {
    eventName: '',
    title: '',
    message: '',
    desktopImageUrl: '',
    mobileImageUrl: '',
    startAt: localDateTime(start),
    endAt: localDateTime(end),
    offerLabel: '',
    voucherCode: '',
    targetUrl: 'products',
    status: 'ACTIVE',
    priority: 0,
  }
}

function eventToForm(event) {
  return {
    ...event,
    startAt: localDateTime(event.startAt),
    endAt: localDateTime(event.endAt),
    message: event.message || '',
    mobileImageUrl: event.mobileImageUrl || '',
    offerLabel: event.offerLabel || '',
    voucherCode: event.voucherCode || '',
  }
}

function eventPayload(form, status = form.status) {
  const fields = Object.fromEntries(Object.entries(form).filter(([key]) => key !== 'id'))
  return {
    ...fields,
    startAt: form.startAt,
    endAt: form.endAt,
    priority: Number(form.priority),
    status,
  }
}

export function CampaignEventManagementPage({ setNotice }) {
  const [events, setEvents] = useState([])
  const [form, setForm] = useState(createEmptyEvent)
  const [editingId, setEditingId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState('')
  const [feedback, setFeedback] = useState('')

  const loadEvents = useCallback(async () => {
    setLoading(true)
    try {
      const rows = await campaignEventService.getAdminCampaignEvents()
      setEvents(Array.isArray(rows) ? rows : [])
    } catch (error) {
      setNotice?.(`Không tải được sự kiện: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }, [setNotice])

  useEffect(() => { loadEvents() }, [loadEvents])

  const activeCount = useMemo(() => events.filter((event) => event.status === 'ACTIVE').length, [events])

  function showFeedback(message) {
    setFeedback(message)
    setNotice?.(message)
  }

  function updateField(name, value) {
    setForm((current) => ({ ...current, [name]: value }))
  }

  function startEdit(event) {
    setEditingId(event.id)
    setForm(eventToForm(event))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function resetForm() {
    setEditingId(null)
    setForm(createEmptyEvent())
  }

  async function uploadImage(field, file) {
    if (!file) return
    if (file.size > 8 * 1024 * 1024) {
      setNotice?.('Ảnh banner tối đa 8 MB.')
      return
    }
    setUploading(field)
    try {
      const result = await campaignEventService.uploadCampaignImage(file)
      updateField(field, result.imageUrl)
      showFeedback('Đã tải ảnh banner lên.')
    } catch (error) {
      showFeedback(`Tải ảnh thất bại: ${error.message}`)
    } finally {
      setUploading('')
    }
  }

  async function saveEvent(event) {
    event.preventDefault()
    setSaving(true)
    try {
      const payload = eventPayload(form)
      const saved = editingId
        ? await campaignEventService.updateCampaignEvent(editingId, payload)
        : await campaignEventService.createCampaignEvent(payload)
      setEvents((current) => {
        const next = editingId
          ? current.map((item) => item.id === saved.id ? saved : item)
          : [...current, saved]
        return next.sort((a, b) => b.priority - a.priority || new Date(a.startAt) - new Date(b.startAt))
      })
      const statusMessage = saved.status === 'ACTIVE'
        ? 'Banner đã xuất bản và đang hiển thị trên trang chủ.'
        : saved.status === 'SCHEDULED'
          ? 'Banner sẽ hiển thị khi đến thời gian bắt đầu.'
          : 'Sự kiện đã lưu ở dạng nháp hoặc ẩn; hãy xuất bản để khách hàng thấy banner.'
      showFeedback(editingId ? `Đã cập nhật sự kiện. ${statusMessage}` : statusMessage)
      resetForm()
      loadEvents()
    } catch (error) {
      showFeedback(`Không lưu được sự kiện: ${error.message}`)
    } finally {
      setSaving(false)
    }
  }

  async function toggleEvent(event) {
    const nextStatus = ['ACTIVE', 'SCHEDULED'].includes(event.status) ? 'HIDDEN' : 'ACTIVE'
    try {
      await campaignEventService.updateCampaignEvent(event.id, eventPayload(eventToForm(event), nextStatus))
      showFeedback(nextStatus === 'HIDDEN' ? 'Đã ẩn sự kiện.' : 'Đã bật sự kiện.')
      loadEvents()
    } catch (error) {
      showFeedback(`Không đổi được trạng thái: ${error.message}`)
    }
  }

  async function endEvent(event) {
    if (!window.confirm(`Kết thúc chiến dịch “${event.eventName}” ngay bây giờ?`)) return
    try {
      await campaignEventService.updateCampaignEvent(event.id, eventPayload(eventToForm(event), 'ENDED'))
      showFeedback('Đã kết thúc sự kiện.')
      loadEvents()
    } catch (error) {
      showFeedback(`Không kết thúc được sự kiện: ${error.message}`)
    }
  }

  async function removeEvent(event) {
    if (!window.confirm(`Xóa sự kiện “${event.eventName}”? Thao tác này không thể hoàn tác.`)) return
    try {
      await campaignEventService.deleteCampaignEvent(event.id)
      setEvents((current) => current.filter((item) => item.id !== event.id))
      if (editingId === event.id) resetForm()
      showFeedback('Đã xóa sự kiện.')
    } catch (error) {
      showFeedback(`Không xóa được sự kiện: ${error.message}`)
    }
  }

  return (
    <div className="campaign-admin-page">
      <section className="campaign-admin-heading">
        <div>
          <p className="eyebrow">CHIẾN DỊCH THƯƠNG HIỆU</p>
          <h2>Banner sự kiện</h2>
          <p>Quản lý lịch, ưu đãi và hình ảnh chiến dịch trên trang chủ.</p>
        </div>
        <div className="campaign-admin-count"><strong>{activeCount}</strong><span>đang diễn ra</span></div>
      </section>

      {feedback && <p className="campaign-admin-feedback" role="status" aria-live="polite">{feedback}</p>}

      <div className="campaign-admin-layout">
        <section className="campaign-admin-editor">
          <div className="campaign-admin-section-title">
            <div>
              <p className="eyebrow">{editingId ? `CHỈNH SỬA #${editingId}` : 'TẠO CHIẾN DỊCH MỚI'}</p>
              <h3>{editingId ? 'Cập nhật sự kiện' : 'Thông tin sự kiện'}</h3>
            </div>
            {editingId && <button type="button" className="ghost" onClick={resetForm}>Tạo mới</button>}
          </div>

          <form className="campaign-admin-form" onSubmit={saveEvent}>
            <div className="campaign-admin-two-columns">
              <label>Tên sự kiện<input required maxLength={120} value={form.eventName} onChange={(e) => updateField('eventName', e.target.value)} placeholder="Ví dụ: Summer Sale 2026" /></label>
              <label>Tiêu đề trên banner<input required maxLength={100} value={form.title} onChange={(e) => updateField('title', e.target.value)} placeholder="Hè rực rỡ, phong cách thăng hoa" /></label>
            </div>
            <label>Thông điệp ngắn<input maxLength={240} value={form.message} onChange={(e) => updateField('message', e.target.value)} placeholder="Làm mới tủ đồ với những thiết kế mùa hè." /></label>

            <div className="campaign-admin-two-columns">
              <ImageUploadField label="Ảnh banner desktop" recommendation="Khuyến nghị ảnh ngang khoảng 800 × 333 px." value={form.desktopImageUrl} busy={uploading === 'desktopImageUrl'} onUpload={(file) => uploadImage('desktopImageUrl', file)} />
              <ImageUploadField label="Ảnh banner mobile (tùy chọn)" recommendation="Khuyến nghị ảnh dọc riêng để tránh cắt chủ thể." value={form.mobileImageUrl} busy={uploading === 'mobileImageUrl'} onUpload={(file) => uploadImage('mobileImageUrl', file)} />
            </div>

            <div className="campaign-admin-preview-wrap">
              <span className="campaign-admin-preview-label">XEM TRƯỚC BANNER</span>
              <div className="campaign-admin-preview" style={form.desktopImageUrl ? { backgroundImage: `linear-gradient(90deg, rgba(28,20,23,.72), rgba(28,20,23,.05)), url("${campaignImageSource(form.desktopImageUrl)}")` } : undefined}>
                <div>
                  <small>{form.eventName || 'TÊN SỰ KIỆN'}</small>
                  <strong>{form.title || 'Tiêu đề chiến dịch'}</strong>
                  {form.message && <span>{form.message}</span>}
                  {form.offerLabel && <b>{form.offerLabel}</b>}
                </div>
              </div>
            </div>

            <div className="campaign-admin-two-columns">
              <label>Bắt đầu<input type="datetime-local" required value={form.startAt} onChange={(e) => updateField('startAt', e.target.value)} /></label>
              <label>Kết thúc<input type="datetime-local" required value={form.endAt} onChange={(e) => updateField('endAt', e.target.value)} /></label>
            </div>
            <p className="campaign-admin-hint">Múi giờ cửa hàng: Việt Nam (UTC+7). Banner chỉ hiện khi sự kiện đã xuất bản và đang trong khoảng thời gian này.</p>
            <div className="campaign-admin-two-columns">
              <label>Nhãn ưu đãi<input maxLength={60} value={form.offerLabel} onChange={(e) => updateField('offerLabel', e.target.value)} placeholder="Giảm 20%" /></label>
              <label>Mã voucher thật (nếu có)<input maxLength={50} value={form.voucherCode} onChange={(e) => updateField('voucherCode', e.target.value.toUpperCase())} placeholder="SUMMER20" /></label>
            </div>
            <p className="campaign-admin-hint">Nhãn phần trăm cần liên kết voucher đang hoạt động và còn hạn trong suốt sự kiện.</p>
            <div className="campaign-admin-two-columns">
              <label>Đường dẫn khi nhấn<input required maxLength={300} value={form.targetUrl} onChange={(e) => updateField('targetUrl', e.target.value)} placeholder="products hoặc home?section=collections" /></label>
              <label>Trạng thái<select value={form.status} onChange={(e) => updateField('status', e.target.value)}>{STATUS_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select><small>Chọn “Xuất bản” để khách hàng thấy banner ngay, hoặc “Lên lịch” để tự hiện khi tới ngày bắt đầu.</small></label>
            </div>
            <label>Độ ưu tiên<input type="number" min="0" max="100000" value={form.priority} onChange={(e) => updateField('priority', e.target.value)} /><small>Ưu tiên cao hiển thị trước trong carousel.</small></label>
            <div className="campaign-admin-submit-row">
              <button type="submit" disabled={saving || Boolean(uploading)}>{saving ? 'Đang lưu…' : editingId ? 'Lưu sự kiện' : 'Tạo sự kiện'}</button>
              <button type="button" className="ghost" onClick={resetForm}>Hủy</button>
            </div>
          </form>
        </section>

        <section className="campaign-admin-list">
          <div className="campaign-admin-section-title">
            <div><p className="eyebrow">LỊCH CHIẾN DỊCH</p><h3>Sự kiện đã tạo</h3></div>
            <button type="button" className="ghost" onClick={loadEvents}>Làm mới</button>
          </div>
          {loading ? <p className="campaign-admin-empty">Đang tải sự kiện…</p> : events.length === 0 ? <p className="campaign-admin-empty">Chưa có sự kiện. Tạo chiến dịch đầu tiên để xuất hiện trên trang chủ.</p> : (
            <div className="campaign-admin-event-list">
              {events.map((event) => (
                <article className="campaign-admin-event" key={event.id}>
                  <div className="campaign-admin-event-image">
                    {event.desktopImageUrl && <img src={campaignImageSource(event.desktopImageUrl)} alt="" />}
                    <span className={`campaign-status status-${event.status.toLowerCase()}`}>{STATUS_LABELS[event.status] || event.status}</span>
                  </div>
                  <div className="campaign-admin-event-info">
                    <small>Ưu tiên {event.priority}</small>
                    <h4>{event.eventName}</h4>
                    <p>{event.title}</p>
                    <time>{new Date(event.startAt).toLocaleString('vi-VN')} — {new Date(event.endAt).toLocaleString('vi-VN')}</time>
                    {event.voucherCode && <span className="campaign-admin-voucher">Voucher {event.voucherCode}</span>}
                  </div>
                  <div className="campaign-admin-event-actions">
                    <button type="button" className="ghost" onClick={() => startEdit(event)}>Sửa</button>
                    <button type="button" className="ghost" disabled={event.status === 'ENDED'} onClick={() => toggleEvent(event)}>{event.status === 'ENDED' ? 'Đã kết thúc' : ['ACTIVE', 'SCHEDULED'].includes(event.status) ? 'Ẩn' : 'Bật'}</button>
                    {['ACTIVE', 'SCHEDULED'].includes(event.status) && <button type="button" className="campaign-end-button" onClick={() => endEvent(event)}>Kết thúc</button>}
                    <button type="button" className="campaign-delete-button" onClick={() => removeEvent(event)}>Xóa</button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

function ImageUploadField({ label, value, busy, onUpload, recommendation }) {
  return (
    <label className="campaign-admin-image-field">
      {label}
      <input type="file" accept="image/jpeg,image/png" onChange={(event) => onUpload(event.target.files?.[0])} />
      <small>{busy ? 'Đang tải ảnh…' : value ? 'Đã có ảnh banner. Chọn tệp khác để thay.' : 'JPEG hoặc PNG, tối đa 8 MB.'}</small>
      <small>{recommendation}</small>
    </label>
  )
}
