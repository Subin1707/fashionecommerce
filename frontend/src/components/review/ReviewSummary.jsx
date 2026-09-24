import { useEffect, useState } from 'react'
import { getProductReviews } from '../../services/reviewService'
import { ReviewMedia } from './ReviewMedia'

export function ReviewSummary({ product }) {
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let cancelled = false
    getProductReviews(product.id)
      .then((rows) => { if (!cancelled) setReviews(rows) })
      .catch((err) => { if (!cancelled) setError(err.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [product.id])
  const average = reviews.length ? reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length : 0
  return <section className="table-panel">
    <h2>Đánh giá sản phẩm</h2>
    {loading ? <p>Đang tải đánh giá...</p> : error ? <p role="alert">Không tải được đánh giá: {error}</p> : <>
      <p><strong>{average.toFixed(1)}/5</strong> từ {reviews.length} đánh giá</p>
      {!reviews.length && <p>Chưa có đánh giá cho sản phẩm này.</p>}
      {reviews.map((review) => <article className="review-card" key={review.id}>
        <strong>Khách hàng #{review.userId} · {review.rating}/5 ★</strong>
        {review.isVerified && <small> · Đã mua hàng</small>}
        <p>{review.comment}</p><ReviewMedia media={review.media} />
        {review.sellerReply && <blockquote><strong>Phản hồi của cửa hàng</strong><p>{review.sellerReply}</p></blockquote>}
      </article>)}
    </>}
  </section>
}
