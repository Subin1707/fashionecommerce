export function ReviewMedia({ media = [] }) {
  return <div className="review-media-grid">{media.map((url, index) => (
    url.startsWith('data:video/')
      ? <video key={index} src={url} controls preload="metadata" aria-label={`Video đánh giá ${index + 1}`} />
      : <img key={index} src={url} alt={`Ảnh đánh giá ${index + 1}`} loading="lazy" />
  ))}</div>
}
