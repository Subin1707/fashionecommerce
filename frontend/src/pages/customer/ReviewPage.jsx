import { useEffect, useState } from 'react'
import * as reviewService from '../../services/reviewService'
import * as orderService from '../../services/orderService'
import { ReviewMedia } from '../../components/review/ReviewMedia'
import { ROUTES } from '../../utils/constants'

export function ReviewPage({ setNotice, setRoute }) {
  const [orders, setOrders] = useState([])
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedId, setSelectedId] = useState(() => new URLSearchParams(window.location.hash.split('?')[1]).get('orderItemId') || '')
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [media, setMedia] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [readingMedia, setReadingMedia] = useState(false)

  useEffect(() => {
    let cancelled = false
    Promise.all([orderService.getOrders(), reviewService.getMyReviews()])
      .then(([rows, myReviews]) => { if (!cancelled) { setOrders(rows); setReviews(myReviews) } })
      .catch((err) => { if (!cancelled) setError(err.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  const purchased = orders.filter((order) => order.orderStatus === 'COMPLETED')
    .flatMap((order) => order.items.map((item) => ({ ...item, orderId: order.id })))
  const eligible = purchased.filter((item) => !reviews.some((review) => String(review.orderItemId) === String(item.id)))
  const selected = eligible.find((item) => String(item.id) === selectedId)

  async function readMedia(event) {
    const files = Array.from(event.target.files || [])
    event.target.value = ''
    if (files.length > 5 || files.reduce((sum, file) => sum + file.size, 0) > 5 * 1024 * 1024) {
      setNotice?.('Tối đa 5 ảnh/video, tổng dung lượng 5 MB.')
      return
    }
    if (files.some((file) => !['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/webm'].includes(file.type))) {
      setNotice?.('Chọn ảnh JPG, PNG, WebP hoặc video MP4, WebM.')
      return
    }
    setReadingMedia(true)
    try {
      const urls = await Promise.all(files.map((file) => new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result)
        reader.onerror = () => reject(new Error('Không đọc được tệp'))
        reader.readAsDataURL(file)
      })))
      setMedia(urls)
    } catch (err) { setNotice?.(err.message) }
    finally { setReadingMedia(false) }
  }

  async function submit(event) {
    event.preventDefault()
    if (!selected || submitting || readingMedia) return
    setSubmitting(true)
    try {
      const review = await reviewService.createProductReview(selected.productId, {
        orderItemId: selected.id, rating, comment: comment.trim(), media,
      })
      setReviews((rows) => [review, ...rows])
      setNotice?.('Đã gửi đánh giá. Nhận xét của bạn đã hiển thị trên trang sản phẩm.')
      setRoute(`${ROUTES.ORDER_DETAIL}?id=${selected.orderId}`)
    } catch (err) { setNotice?.(`Không gửi được đánh giá: ${err.message}`) }
    finally { setSubmitting(false) }
  }

  if (loading) return <p className="empty-state">Đang tải sản phẩm đã mua...</p>
  if (error) return <p role="alert">Không tải được đơn hàng: {error}</p>

  return <section className="review-workspace">
    <div className="review-heading">
      <p className="eyebrow">Đánh giá từ đơn mua</p>
      <h1>Chia sẻ trải nghiệm của bạn</h1>
      <p>Mỗi sản phẩm trong đơn hoàn thành được đánh giá một lần.</p>
      <button type="button" className="ghost" onClick={() => setRoute(ROUTES.ORDERS)}>Về đơn mua</button>
    </div>
    {!selected ? <p className="empty-state">Sản phẩm này không còn trong danh sách chờ đánh giá. Hãy mở đánh giá từ chi tiết đơn hàng.</p> : (
      <form className="review-compose-panel" onSubmit={submit}>
        <fieldset disabled={submitting || readingMedia} className="review-form-fields">
          <div className="review-product-context">
            {selected.imageUrl && <img src={selected.imageUrl} alt={selected.productName} />}
            <div>
              <strong>{selected.productName}</strong>
              <p>{selected.color} / {selected.size} · Số lượng: {selected.quantity}</p>
              <small>Đơn hàng #{selected.orderId}</small>
            </div>
          </div>
            <div className="rating-picker" role="group" aria-label="Chọn số sao">
              {[1, 2, 3, 4, 5].map((value) => <button key={value} type="button" aria-label={`${value} sao`} aria-pressed={rating === value} className={rating >= value ? 'active' : ''} onClick={() => setRating(value)}>★</button>)}
              <span>{rating}/5 sao</span>
            </div>
            <label>Nhận xét<textarea required maxLength={1000} value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Chia sẻ về chất liệu, màu sắc và độ vừa vặn..." /></label>
            <label>Ảnh/video (tùy chọn)
              <input type="file" multiple accept="image/jpeg,image/png,image/webp,video/mp4,video/webm" onChange={readMedia} />
              <small>Tối đa 5 tệp, tổng 5 MB. Chọn lại để thay các tệp đã chọn.</small>
            </label>
            <ReviewMedia media={media} />
            {media.length > 0 && <button type="button" className="ghost" onClick={() => setMedia([])}>Bỏ ảnh/video</button>}
        </fieldset>
        <button type="submit" disabled={!selected || !comment.trim() || submitting || readingMedia}>{submitting ? 'Đang gửi...' : readingMedia ? 'Đang đọc tệp...' : 'Gửi đánh giá'}</button>
      </form>
    )}
    <h2>Đánh giá đã gửi</h2>
    {reviews.length === 0 && <p>Bạn chưa gửi đánh giá nào.</p>}
    {reviews.map((review) => <article key={review.id} className="review-card">
      <strong>{purchased.find((item) => item.id === review.orderItemId)?.productName || `Sản phẩm #${review.productId}`} · {review.rating}/5 sao</strong>
      <p>Đơn #{review.orderId || '—'} · {({ APPROVED: 'Đã hiển thị', PENDING: 'Chờ duyệt', REJECTED: 'Đã ẩn' })[review.status]}</p>
      <p>{review.comment}</p><ReviewMedia media={review.media} />
      {review.sellerReply && <blockquote><strong>Phản hồi của cửa hàng</strong><p>{review.sellerReply}</p></blockquote>}
      <button type="button" className="ghost" onClick={() => setRoute(`${ROUTES.PRODUCT_DETAIL}?id=${review.productId}`)}>Xem trên trang sản phẩm</button>
    </article>)}
  </section>
}
