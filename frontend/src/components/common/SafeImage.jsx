import { useState } from 'react'

function optimizeImageUrl(src) {
  if (typeof src !== 'string') return src

  try {
    const url = new URL(src)
    if (url.protocol !== 'https:') return src

    if (url.hostname === 'images.unsplash.com') {
      url.searchParams.set('auto', 'format')
      url.searchParams.set('fit', url.searchParams.get('fit') || 'crop')
      const width = Number(url.searchParams.get('w'))
      if (!width || width > 720) url.searchParams.set('w', '720')
      const quality = Number(url.searchParams.get('q'))
      if (!quality || quality > 70) url.searchParams.set('q', '70')
      return url.toString()
    }

    // Wikimedia URLs in the Vietnamese catalog already point to 500px
    // thumbnails. Preserve the Commons URL instead of proxying it again.
    if (url.hostname === 'upload.wikimedia.org') return src

    // Vendor product photos have no consistent resize API. Use a cached image
    // transformer to cap their dimensions and encode them as compact WebP.
    const optimized = new URL('https://wsrv.nl/')
    optimized.searchParams.set('url', url.toString())
    optimized.searchParams.set('w', '640')
    optimized.searchParams.set('h', '800')
    optimized.searchParams.set('fit', 'inside')
    optimized.searchParams.set('we', '')
    optimized.searchParams.set('output', 'webp')
    optimized.searchParams.set('q', '68')
    return optimized.toString()
  } catch {
    return src
  }
}

export function SafeImage({ src, alt = '', fallbackLabel = 'Fashion', className = '', loading = 'lazy', fetchPriority }) {
  const optimizedSrc = optimizeImageUrl(src)
  const [failedSource, setFailedSource] = useState(null)
  const [unoptimizedSource, setUnoptimizedSource] = useState(null)
  const displaySrc = unoptimizedSource === optimizedSrc ? src : optimizedSrc
  const hasFailed = failedSource === optimizedSrc

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

  return (
    <img
      className={className}
      src={displaySrc}
      alt={alt}
      loading={loading}
      decoding="async"
      fetchPriority={fetchPriority}
      width="640"
      height="800"
      onError={() => {
        if (displaySrc !== src) setUnoptimizedSource(optimizedSrc)
        else setFailedSource(optimizedSrc)
      }}
    />
  )
}
