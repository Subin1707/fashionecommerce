import { useCallback, useEffect, useMemo, useState } from 'react'
import * as reviewService from '../../services/reviewService'
import { ReviewMedia } from '../../components/review/ReviewMedia'

const STATUS_LABELS = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
}

const STATUS_TONES = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
}

export function ReviewManagementPage({ setNotice }) {
  const [reviews, setReviews] = useState([])
  const [selectedReview, setSelectedReview] = useState(null)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [ratingFilter, setRatingFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [productIdInput, setProductIdInput] = useState('')

  const loadData = useCallback(async function loadData(productId) {
    setLoading(true)
    try {
      let rows
      if (productId && String(productId).trim()) {
        rows = (await reviewService.getAllReviews()).filter((review) => String(review.productId) === String(productId).trim())
      } else {
        rows = await reviewService.getAllReviews()
      }
      setReviews(Array.isArray(rows) ? rows : [])
    } catch (error) {
      setNotice?.(`Chưa đọc được đánh giá: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }, [setNotice])

  useEffect(() => {
    Promise.resolve().then(() => loadData())
  }, [loadData])

  const summary = useMemo(() => ({
    total: reviews.length,
    pending: reviews.filter((r) => r.status === 'PENDING').length,
    approved: reviews.filter((r) => r.status === 'APPROVED').length,
    rejected: reviews.filter((r) => r.status === 'REJECTED').length,
    avgRating: reviews.length
      ? (reviews.reduce((sum, r) => sum + Number(r.rating || 0), 0) / reviews.length).toFixed(1)
      : '--',
  }), [reviews])

  const filteredReviews = useMemo(() => {
    const q = query.trim().toLowerCase()
    return reviews
      .filter((r) => {
        const matchQuery = !q
          || String(r.id).includes(q)
          || String(r.productId).includes(q)
          || String(r.userId).includes(q)
          || r.comment?.toLowerCase().includes(q)
        const matchStatus = statusFilter === 'ALL' || r.status === statusFilter
        const matchRating = ratingFilter === 'ALL' || Number(r.rating) === Number(ratingFilter)
        return matchQuery && matchStatus && matchRating
      })
      .sort((a, b) => {
        if (a.status === 'PENDING' && b.status !== 'PENDING') return -1
        if (b.status === 'PENDING' && a.status !== 'PENDING') return 1
        return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
      })
  }, [reviews, query, statusFilter, ratingFilter])

  async function handleSetStatus(reviewId, status) {
    setActionLoading(true)
    try {
      const updated = await reviewService.updateReviewStatus(reviewId, status)
      setReviews((rows) => rows.map((r) => (r.id === reviewId ? updated : r)))
      if (selectedReview?.id === reviewId) setSelectedReview(updated)
      setNotice?.(`Đã ${STATUS_LABELS[status] || status} đánh giá #${reviewId}.`)
    } catch (error) {
      setNotice?.(`Không cập nhật được đánh giá: ${error.message}`)
    } finally {
      setActionLoading(false)
    }
  }

  async function handleDelete(reviewId) {
    if (!window.confirm('Bạn có chắc muốn xóa đánh giá này?')) return
    setActionLoading(true)
    try {
      await reviewService.deleteReview(reviewId)
      setReviews((rows) => rows.filter((r) => r.id !== reviewId))
      if (selectedReview?.id === reviewId) setSelectedReview(null)
      setNotice?.(`Đã xóa đánh giá #${reviewId}.`)
    } catch (error) {
      setNotice?.(`Không xóa được đánh giá: ${error.message}`)
    } finally {
      setActionLoading(false)
    }
  }

  async function handleReply(reviewId, reply) {
    setActionLoading(true)
    try {
      const updated = await reviewService.replyToReview(reviewId, reply)
      setReviews((rows) => rows.map((row) => row.id === updated.id ? updated : row))
      setSelectedReview(updated)
      setNotice?.('Đã gửi phản hồi của cửa hàng.')
    } catch (error) { setNotice?.(`Không gửi được phản hồi: ${error.message}`) }
    finally { setActionLoading(false) }
  }

  function handleProductFilter(event) {
    event.preventDefault()
    loadData(productIdInput)
  }

  return (
    <section className="review-admin-page">
      <div className="review-admin-hero">
        <div>
          <p className="eyebrow">Quản trị nội dung</p>
          <h1>Quản lý đánh giá</h1>
          <p>Kiểm duyệt, phê duyệt hoặc từ chối các đánh giá của khách hàng. Giữ cho chất lượng nội dung luôn đáng tin cậy và có ích.</p>
        </div>
        <div className="review-admin-hero-actions">
          <form className="review-admin-product-form" onSubmit={handleProductFilter}>
            <input
              value={productIdInput}
              onChange={(e) => setProductIdInput(e.target.value)}
              placeholder="Lọc theo Product ID..."
            />
            <button type="submit" disabled={loading}>{loading ? '...' : 'Lọc'}</button>
          </form>
          <button type="button" className="ghost" onClick={() => { setProductIdInput(''); loadData() }} disabled={loading}>
            Tất cả
          </button>
        </div>
      </div>

      <div className="review-admin-kpi-grid">
        <ReviewKpi label="Tổng đánh giá" value={summary.total} />
        <ReviewKpi label="Chờ duyệt" value={summary.pending} tone="warning" />
        <ReviewKpi label="Đã duyệt" value={summary.approved} tone="success" />
        <ReviewKpi label="Từ chối" value={summary.rejected} tone="danger" />
        <ReviewKpi label="Điểm TB" value={summary.avgRating} icon="★" />
      </div>

      <div className="review-admin-toolbar">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm theo bình luận, ID sản phẩm, ID người dùng..."
        />
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="ALL">Tất cả trạng thái</option>
          <option value="PENDING">Chờ duyệt</option>
          <option value="APPROVED">Đã duyệt</option>
          <option value="REJECTED">Từ chối</option>
        </select>
        <select value={ratingFilter} onChange={(e) => setRatingFilter(e.target.value)}>
          <option value="ALL">Tất cả điểm</option>
          <option value="5">5 sao</option>
          <option value="4">4 sao</option>
          <option value="3">3 sao</option>
          <option value="2">2 sao</option>
          <option value="1">1 sao</option>
        </select>
        <button type="button" className="ghost" onClick={() => loadData(productIdInput)} disabled={loading}>
          {loading ? 'Đang tải...' : 'Làm mới'}
        </button>
      </div>

      <div className="review-admin-layout">
        <div className="review-admin-list-panel">
          <div className="review-admin-panel-heading">
            <div>
              <p className="eyebrow">Danh sách đánh giá</p>
              <h2>{filteredReviews.length} kết quả</h2>
            </div>
            {summary.pending > 0 && (
              <span className="review-admin-pending-badge">{summary.pending} chờ duyệt</span>
            )}
          </div>

          {loading && <div className="admin-product-loading" />}
          {!loading && filteredReviews.length === 0 && (
            <p className="empty-state">Không tìm thấy đánh giá nào phù hợp.</p>
          )}

          <div className="review-admin-list">
            {!loading && filteredReviews.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                selected={selectedReview?.id === review.id}
                actionLoading={actionLoading}
                onSelect={() => setSelectedReview(review)}
                onApprove={() => handleSetStatus(review.id, 'APPROVED')}
                onReject={() => handleSetStatus(review.id, 'REJECTED')}
                onDelete={() => handleDelete(review.id)}
              />
            ))}
          </div>
        </div>

        <aside className="review-admin-detail-panel">
          {selectedReview ? (
            <ReviewDetail
              key={selectedReview.id}
              review={selectedReview}
              onReply={(reply) => handleReply(selectedReview.id, reply)}
              actionLoading={actionLoading}
              onApprove={() => handleSetStatus(selectedReview.id, 'APPROVED')}
              onReject={() => handleSetStatus(selectedReview.id, 'REJECTED')}
              onDelete={() => handleDelete(selectedReview.id)}
              onClose={() => setSelectedReview(null)}
            />
          ) : (
            <div className="review-admin-empty-detail">
              <span className="review-admin-empty-icon">💬</span>
              <p className="eyebrow">Chi tiết đánh giá</p>
              <h2>Chọn một đánh giá</h2>
              <p>Nhấn vào bất kỳ đánh giá nào để xem chi tiết và thực hiện kiểm duyệt.</p>
              {summary.pending > 0 && (
                <button type="button" onClick={() => {
                  setStatusFilter('PENDING')
                  const first = reviews.find((r) => r.status === 'PENDING')
                  if (first) setSelectedReview(first)
                }}>
                  Duyệt {summary.pending} đánh giá đang chờ
                </button>
              )}
            </div>
          )}
        </aside>
      </div>
    </section>
  )
}

function ReviewKpi({ label, value, tone = '', icon }) {
  return (
    <article className={`review-admin-kpi ${tone}`}>
      <span>{label}</span>
      <strong>{icon && <em>{icon} </em>}{value}</strong>
    </article>
  )
}

function ReviewCard({ review, selected, actionLoading, onSelect, onApprove, onReject, onDelete }) {
  const stars = renderStars(review.rating)
  const tone = STATUS_TONES[review.status] || ''
  const label = STATUS_LABELS[review.status] || review.status

  return (
    <article
      className={`review-admin-card ${selected ? 'selected' : ''} ${tone}`}
      onClick={onSelect}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && onSelect()}
    >
      <div className="review-admin-card-head">
        <div className="review-admin-card-meta">
          <span className="review-admin-stars">{stars}</span>
          <span className="review-admin-date">{formatDate(review.createdAt)}</span>
        </div>
        <span className={`review-admin-status-badge ${tone}`}>{label}</span>
      </div>
      <p className="review-admin-comment">{review.comment || '(Không có bình luận)'}</p>
      <div className="review-admin-card-footer">
        <span>SP #{review.productId}</span>
        <span>User #{review.userId}</span>
        <div className="review-admin-card-actions" onClick={(e) => e.stopPropagation()}>
          {review.status !== 'APPROVED' && (
            <button type="button" className="success-btn" onClick={onApprove} disabled={actionLoading} title="Duyệt">✓</button>
          )}
          {review.status !== 'REJECTED' && (
            <button type="button" className="danger-outline" onClick={onReject} disabled={actionLoading} title="Từ chối">✕</button>
          )}
          <button type="button" className="danger" onClick={onDelete} disabled={actionLoading} title="Xóa">🗑</button>
        </div>
      </div>
    </article>
  )
}

function ReviewDetail({ review, actionLoading, onApprove, onReject, onDelete, onClose, onReply }) {
  const [reply, setReply] = useState(review.sellerReply || '')
  const tone = STATUS_TONES[review.status] || ''
  const label = STATUS_LABELS[review.status] || review.status

  return (
    <div className="review-admin-detail">
      <div className="review-admin-panel-heading">
        <div>
          <p className="eyebrow">Chi tiết #{review.id}</p>
          <h2>Kiểm duyệt nội dung</h2>
        </div>
        <button type="button" className="ghost" onClick={onClose}>✕</button>
      </div>

      <div className="review-admin-detail-body">
        <div className="review-admin-detail-status">
          <span className={`review-admin-status-badge ${tone} large`}>{label}</span>
        </div>
        <div className="review-admin-detail-rating">
          <span className="review-admin-stars large">{renderStars(review.rating)}</span>
          <span className="review-admin-rating-num">{review.rating} / 5</span>
        </div>
        <div className="review-admin-detail-comment">
          <p className="eyebrow">Nội dung bình luận</p>
          <blockquote>{review.comment || '(Không có bình luận)'}</blockquote>
          <ReviewMedia media={review.media} />
          <p>Đơn hàng #{review.orderId || '—'} · {review.isVerified ? 'Đã xác minh mua hàng' : 'Đánh giá cũ'}</p>
          <form onSubmit={(event) => { event.preventDefault(); onReply(reply) }}>
            <label>Phản hồi của cửa hàng
              <textarea required maxLength={1000} value={reply} onChange={(event) => setReply(event.target.value)} disabled={actionLoading} />
            </label>
            <button type="submit" disabled={actionLoading || !reply.trim()}>{actionLoading ? 'Đang gửi...' : 'Gửi phản hồi'}</button>
          </form>
        </div>
        <div className="review-admin-detail-info">
          <div><span>Sản phẩm</span><strong>#{review.productId}</strong></div>
          <div><span>Người dùng</span><strong>#{review.userId}</strong></div>
          <div><span>Thời gian</span><strong>{formatDate(review.createdAt, true)}</strong></div>
          <div><span>Trạng thái</span><strong className={tone}>{label}</strong></div>
        </div>
      </div>

      <div className="review-admin-detail-actions">
        {review.status !== 'APPROVED' && (
          <button type="button" onClick={onApprove} disabled={actionLoading}>
            {actionLoading ? '...' : '✓ Duyệt đánh giá'}
          </button>
        )}
        {review.status !== 'REJECTED' && (
          <button type="button" className="danger-outline" onClick={onReject} disabled={actionLoading}>
            {actionLoading ? '...' : '✕ Từ chối'}
          </button>
        )}
        <button type="button" className="ghost" style={{color:'#c0392b'}} onClick={onDelete} disabled={actionLoading}>
          Xóa vĩnh viễn
        </button>
      </div>
    </div>
  )
}

function renderStars(rating) {
  const n = Math.max(0, Math.min(5, Number(rating) || 0))
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

function formatDate(iso, withTime = false) {
  if (!iso) return '--'
  try {
    const d = new Date(iso)
    if (isNaN(d.getTime())) return String(iso).slice(0, 10)
    if (withTime) return d.toLocaleString('vi-VN')
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
  } catch {
    return String(iso).slice(0, 10)
  }
}
