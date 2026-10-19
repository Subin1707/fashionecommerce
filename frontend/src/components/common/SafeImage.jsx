import { useState } from 'react'

export function SafeImage({ src, alt = '', fallbackLabel = 'Fashion', className = '', loading = 'lazy' }) {
  const [failedSource, setFailedSource] = useState(null)
  const hasFailed = failedSource === src

  if (!src || hasFailed) {
    return (
      <span
        className={`safe-image-unavailable ${className}`}
        role="img"
        aria-label={`${alt || fallbackLabel} — ảnh sản phẩm chưa có`}
      >
        <span>{fallbackLabel}</span>
        {alt && <strong>{alt}</strong>}
      </span>
    )
  }

  return <img className={className} src={src} alt={alt} loading={loading} decoding="async" onError={() => setFailedSource(src)} />
}
