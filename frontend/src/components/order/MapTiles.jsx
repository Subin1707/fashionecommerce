import { useEffect, useRef, useState } from 'react'
import { TileLayer } from 'react-leaflet'

const cartoKey = import.meta.env.VITE_CARTO_API_KEY?.trim()
const tileSources = [
  ...(cartoKey ? [{
    url: `https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png?key=${encodeURIComponent(cartoKey)}`,
    attribution: '&copy; <a href="https://carto.com/attributions">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  }] : []),
  {
    url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  },
  {
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
    attribution: 'Tiles &copy; <a href="https://www.esri.com/">Esri</a> &mdash; Source: Esri, OpenStreetMap contributors',
  },
]

export function MapTiles() {
  const [failed, setFailed] = useState(false)
  const [sourceIndex, setSourceIndex] = useState(0)
  const loaded = useRef(false)
  const fallbackTimer = useRef(null)
  const source = tileSources[sourceIndex]

  useEffect(() => {
    loaded.current = false
    clearTimeout(fallbackTimer.current)
    fallbackTimer.current = setTimeout(() => {
      if (!loaded.current && sourceIndex < tileSources.length - 1) setSourceIndex((value) => value + 1)
    }, 3500)
    return () => clearTimeout(fallbackTimer.current)
  }, [sourceIndex])

  function retry() {
    setFailed(false)
    loaded.current = false
    setSourceIndex((value) => value < tileSources.length - 1 ? value + 1 : 0)
  }

  return <>
    <TileLayer key={source.url}
      attribution={source.attribution}
      url={source.url} maxZoom={19}
      eventHandlers={{
        tileload: () => { loaded.current = true },
        tileerror: () => {
          if (sourceIndex < tileSources.length - 1) retry()
          else setFailed(true)
        },
      }} />
    {failed && <div className="map-network-notice" role="status">
      <span>Một phần bản đồ chưa tải được. Kiểm tra kết nối và thử lại.</span>
      <button type="button" onClick={retry}>Tải lại</button>
    </div>}
  </>
}
