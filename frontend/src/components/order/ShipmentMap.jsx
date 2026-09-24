import { useEffect, useMemo } from 'react'
import { divIcon } from 'leaflet'
import { MapContainer, Marker, Polyline, Popup, ZoomControl, useMap } from 'react-leaflet'
import { MapResize } from '../cart/DeliveryLocationPicker'
import { MapTiles } from './MapTiles'

const icon = (symbol) => divIcon({ className: 'shipment-map-icon', html: `<span>${symbol}</span>`, iconSize: [36, 36], iconAnchor: [18, 30] })
const shopIcon = icon('🏪'), truckIcon = icon('🚚'), homeIcon = icon('🏠')

function FitRoute({ positions }) {
  const map = useMap()
  const boundsKey = JSON.stringify(positions)
  useEffect(() => {
    const bounds = JSON.parse(boundsKey)
    if (bounds.length) map.fitBounds(bounds, { padding: [35, 35], maxZoom: 16 })
  }, [map, boundsKey])
  return null
}

export function ShipmentMap({ shipment, route = [] }) {
  const positions = useMemo(() => route.map((point) => [point.latitude, point.longitude]), [route])
  if ([shipment?.currentLat, shipment?.currentLng, shipment?.pickupLat, shipment?.pickupLng,
    shipment?.deliveryLat, shipment?.deliveryLng].some((value) => value == null)) return null
  const pickup = [shipment.pickupLat, shipment.pickupLng]
  const current = [shipment.currentLat, shipment.currentLng]
  const destination = [shipment.deliveryLat, shipment.deliveryLng]
  return <>
    <div className="shipment-map" style={{ height: 420 }}>
      <MapContainer center={current} zoom={13} zoomControl={false} scrollWheelZoom={false} style={{ height: '100%', width: '100%' }}>
        <MapTiles />
        <ZoomControl position="bottomright" />
        <MapResize />
        <FitRoute positions={positions.length ? positions : [pickup, destination]} />
        <Marker position={pickup} icon={shopIcon}><Popup>Fashion Shop</Popup></Marker>
        <Marker position={current} icon={truckIcon}><Popup>Vị trí xe mô phỏng</Popup></Marker>
        <Marker position={destination} icon={homeIcon}><Popup>Địa chỉ nhận hàng</Popup></Marker>
        {positions.length > 1 && <>
          <Polyline positions={positions} pathOptions={{ color: '#fff', weight: 9, opacity: .9 }} />
          <Polyline positions={positions} pathOptions={{ color: '#9d533b', weight: 5, opacity: .95, lineCap: 'round' }} />
        </>}
      </MapContainer>
    </div>
    {!positions.length && <p>Vận đơn này chưa có tuyến đường được lưu.</p>}
    <small>Xe được mô phỏng trên tuyến đường thực, chưa kết nối GPS của hãng vận chuyển.</small>
  </>
}
