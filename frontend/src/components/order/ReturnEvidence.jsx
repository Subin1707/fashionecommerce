import { useEffect, useState } from 'react'
import { fetchAuthenticatedBlob } from '../../services/api'

export function ReturnEvidence({ files = [] }) {
  const [media, setMedia] = useState([])
  useEffect(() => {
    let active = true
    const urls = []
    Promise.all(files.map(async (path) => {
      const url = await fetchAuthenticatedBlob(path)
      if (active) urls.push(url)
      else URL.revokeObjectURL(url)
      return { path, url }
    })).then((rows) => { if (active) setMedia(rows) })
      .catch(() => {
        urls.forEach((url) => URL.revokeObjectURL(url))
        urls.length = 0
        if (active) setMedia([])
      })
    return () => {
      active = false
      urls.forEach((url) => URL.revokeObjectURL(url))
    }
  }, [files])

  if (!files.length) return null
  return <div className="return-evidence-grid" aria-label="Tệp minh chứng">
    {media.map(({ path, url }) => /\.(mp4|webm)$/i.test(path)
      ? <video key={path} src={url} controls preload="metadata" />
      : <a key={path} href={url} target="_blank" rel="noreferrer"><img src={url} alt="Ảnh minh chứng đổi trả" loading="lazy" /></a>)}
  </div>
}
