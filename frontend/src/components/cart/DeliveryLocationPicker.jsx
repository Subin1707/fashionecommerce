import { useEffect, useRef, useState } from 'react'
import { MapContainer, ZoomControl, useMap, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { apiRequest } from '../../services/api'
import { MapTiles } from '../order/MapTiles'
import './DeliveryLocationPicker.css'

function MapIcon({ name = 'pin' }) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {name === 'search' ? <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4 4" /></>
      : name === 'locate' ? <><circle cx="12" cy="12" r="7" /><circle cx="12" cy="12" r="2" /><path d="M12 2v3m0 14v3M2 12h3m14 0h3" /></>
        : name === 'check' ? <path d="m5 12 4 4L19 6" />
          : <><path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></>}
  </svg>
}

const DEFAULT_CENTER = [21.0075, 105.8412]

function MapCenterListener({ onMove, onMoving, target }) {
  const map = useMapEvents({
    movestart: onMoving,
    moveend() {
      const center = map.getCenter()
      onMove(center.lat, center.lng)
    },
  })
  useEffect(() => {
    if (target) map.flyTo([target.latitude, target.longitude], 17, { duration: 0.8 })
  }, [map, target])
  return null
}

export function MapResize() {
  const map = useMap()
  useEffect(() => {
    const observer = new ResizeObserver(() => map.invalidateSize())
    observer.observe(map.getContainer())
    map.invalidateSize()
    return () => observer.disconnect()
  }, [map])
  return null
}

export function DeliveryLocationPicker({ latitude, longitude, address, onChange }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState('')
  const [target, setTarget] = useState(null)
  const [draft, setDraft] = useState(null)
  const [resolving, setResolving] = useState(false)
  const [locating, setLocating] = useState(false)
  const [locationAccuracy, setLocationAccuracy] = useState(null)
  const mounted = useRef(false)
  const timer = useRef(null)
  const revision = useRef(0)
  const searchRevision = useRef(0)
  const selectedTarget = useRef(null)
  const [initial] = useState(() => latitude != null && longitude != null ? [latitude, longitude] : DEFAULT_CENTER)
  const center = useRef(initial)
  const confirmed = draft && latitude === draft.latitude && longitude === draft.longitude && address === draft.displayName

  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
      clearTimeout(timer.current)
    }
  }, [])

  function locate() {
    if (!navigator.geolocation) { setError('Trình duyệt chưa hỗ trợ định vị. Bạn có thể tìm địa chỉ bên dưới.'); return }
    if (!window.isSecureContext) {
      setError('Trình duyệt chỉ cho phép định vị trên HTTPS hoặc localhost. Hãy mở trang bằng HTTPS hoặc nhập địa chỉ để tìm kiếm.')
      return
    }
    setLocating(true)
    setError('')
    navigator.geolocation.getCurrentPosition(({ coords }) => {
      if (!mounted.current) return
      revision.current++
      clearTimeout(timer.current)
      selectedTarget.current = null
      const location = {
        latitude: coords.latitude,
        longitude: coords.longitude,
      }
      setLocationAccuracy(coords.accuracy)
      setDraft({
        ...location,
        displayName: `Vị trí thiết bị (độ chính xác khoảng ${Math.round(coords.accuracy)} m)`,
      })
      setResolving(false)
      setError('')
      center.current = [coords.latitude, coords.longitude]
      setTarget({ latitude: coords.latitude, longitude: coords.longitude })
      setLocating(false)
    }, (positionError) => {
      if (!mounted.current) return
      const messages = {
        1: 'Quyền truy cập vị trí đang bị chặn. Hãy cho phép Location cho trang web trong biểu tượng cài đặt cạnh thanh địa chỉ, rồi bấm “Vị trí của tôi” lần nữa.',
        2: 'Thiết bị chưa xác định được vị trí. Hãy bật dịch vụ vị trí/GPS hoặc tìm địa chỉ trên bản đồ.',
        3: 'Lấy vị trí quá thời gian chờ. Hãy bật GPS, thử lại hoặc tìm địa chỉ trên bản đồ.',
      }
      setError(messages[positionError.code] || 'Chưa lấy được vị trí. Hãy cho phép truy cập vị trí hoặc nhập địa chỉ để tìm kiếm.')
      setLocating(false)
    }, { timeout: 30000, maximumAge: 0, enableHighAccuracy: true })
  }

  function moving() {
    if (selectedTarget.current) return
    setLocationAccuracy(null)
    revision.current++
    clearTimeout(timer.current)
    setDraft(null)
    setResolving(true)
    if (latitude != null || longitude != null) onChange(null, null, address)
  }

  function moved(lat, lng) {
    center.current = [lat, lng]
    if (selectedTarget.current) {
      const selected = selectedTarget.current
      selectedTarget.current = null
      setDraft(selected)
      setResolving(false)
      return
    }
    const version = ++revision.current
    clearTimeout(timer.current)
    setDraft(null)
    setResolving(true)
    setError('')
    timer.current = setTimeout(async () => {
      try {
        const result = await apiRequest(`/api/location/reverse?lat=${lat}&lng=${lng}`)
        if (mounted.current && version === revision.current) setDraft({ ...result, latitude: lat, longitude: lng })
      } catch (err) {
        if (mounted.current && version === revision.current) {
          setDraft({
            displayName: `Vị trí đã chọn (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
            latitude: lat,
            longitude: lng,
          })
          setError(`Không lấy được tên địa chỉ tự động: ${err.message} Tọa độ ghim vẫn được giữ; hãy nhập địa chỉ giao hàng nếu cần rồi xác nhận.`)
        }
      } finally {
        if (mounted.current && version === revision.current) setResolving(false)
      }
    }, 750)
  }

  async function search() {
    const version = ++searchRevision.current
    setSearching(true)
    setError('')
    setResults([])
    try {
      const rows = await apiRequest(`/api/location/search?q=${encodeURIComponent(query.trim())}`)
      if (!mounted.current || version !== searchRevision.current) return
      setResults(rows)
      if (!rows.length) setError('Không tìm thấy địa chỉ. Thử thêm tên phường hoặc thành phố.')
    } catch (err) {
      if (mounted.current && version === searchRevision.current) setError(err.message)
    } finally {
      if (mounted.current && version === searchRevision.current) setSearching(false)
    }
  }

  return <div className="delivery-location-picker">
    <div className="delivery-map-heading">
      <div className="delivery-heading-copy"><span className="delivery-heading-icon"><MapIcon /></span>
        <div><strong>Giao đến đúng nơi bạn muốn</strong><p>Tìm địa chỉ và tinh chỉnh vị trí trên bản đồ.</p></div>
      </div>
      <span className={`delivery-map-status ${confirmed ? 'is-confirmed' : ''}`}><i />{confirmed ? 'Đã xác nhận' : 'Chọn điểm giao'}</span>
    </div>
    <div className="delivery-search">
      <MapIcon name="search" />
      <input aria-label="Tìm địa chỉ" maxLength={200} value={query} placeholder="Tên đường, địa điểm, thành phố"
        onChange={(event) => setQuery(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault()
            if (query.trim().length >= 3 && !searching) search()
          }
        }} />
      <button type="button" disabled={searching || query.trim().length < 3} onClick={search}>
        {searching ? 'Đang tìm...' : 'Tìm kiếm'}
      </button>
    </div>
    {!!results.length && <ul className="delivery-results">
      {results.map((result, index) => <li key={index}>
        <button type="button" onClick={() => {
          selectedTarget.current = result
          setLocationAccuracy(null)
          setDraft(result)
          setResolving(false)
          setTarget(result)
          setResults([])
          setQuery(result.displayName)
        }}>
          <MapIcon /><span>{result.displayName}</span><span aria-hidden="true">↗</span>
        </button>
      </li>)}
    </ul>}
    <div className="delivery-map-wrapper">
      <MapContainer center={initial} zoom={16} className="delivery-map" zoomControl={false} scrollWheelZoom={false}>
        <MapTiles />
        <ZoomControl position="bottomright" />
        <MapResize />
        <MapCenterListener onMove={moved} onMoving={moving} target={target} />
      </MapContainer>
      <div className="delivery-map-hint">Kéo bản đồ để điều chỉnh điểm giao</div>
      <button className="delivery-locate" type="button" disabled={locating} onClick={locate}>
        <MapIcon name="locate" />{locating ? 'Đang định vị...' : 'Vị trí của tôi'}
      </button>
      <div className={`delivery-fixed-pin ${resolving ? 'is-moving' : ''}`} aria-hidden="true">
        <span className="delivery-pin-label">Giao hàng tại đây</span>
        <svg width="44" height="56" viewBox="0 0 44 56" fill="none"><path d="M22 53S3 32 3 22a19 19 0 0 1 38 0c0 10-19 31-19 31Z" fill="#9d533b" stroke="white" strokeWidth="3" /><circle cx="22" cy="22" r="7" fill="white" /></svg>
        <span className="delivery-pin-shadow" />
      </div>
    </div>
    <div className={`delivery-address-card ${confirmed ? 'is-confirmed' : ''}`} aria-live="polite">
      <span className="delivery-address-icon"><MapIcon name={confirmed ? 'check' : 'pin'} /></span>
      <div className="delivery-address-copy"><span className="delivery-address-eyebrow">{confirmed ? 'SẴN SÀNG NHẬN HÀNG' : 'ĐỊA CHỈ NHẬN HÀNG'}</span>
        {resolving ? <p>Đang xác định địa chỉ<span className="delivery-loading-dots">...</span></p> : <p>{draft?.displayName || address || 'Bạn muốn nhận hàng ở đâu?'}</p>}
        {draft && <span className="delivery-address-help">
          Tọa độ ghim: {draft.latitude.toFixed(6)}, {draft.longitude.toFixed(6)}
          {locationAccuracy != null && ` · GPS sai số khoảng ${Math.round(locationAccuracy)} m`}
        </span>}
        {!draft && !resolving && <span className="delivery-address-help">Tìm một địa điểm hoặc chọn vị trí ngay dưới ghim.</span>}
      </div>
      <button className="delivery-confirm" type="button" disabled={!draft || resolving || confirmed} onClick={() => {
        onChange(draft.latitude, draft.longitude, draft.displayName)
      }}><MapIcon name="check" />{confirmed ? 'Đã xác nhận' : 'Xác nhận địa chỉ'}</button>
    </div>
    {error && <p className="delivery-map-error" role="alert">{error}</p>}
    <div className="delivery-map-footer">
      {!resolving && !draft && <button type="button" onClick={() => moved(...center.current)}><MapIcon name="locate" />Lấy địa chỉ tại ghim</button>}
      <small>Dữ liệu địa chỉ © <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">OpenStreetMap</a></small>
    </div>
  </div>
}
