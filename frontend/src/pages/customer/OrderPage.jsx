import { useEffect, useState } from 'react'
import { DataTable } from '../../components/common/DataTable'
import { getStoredAuth } from '../../services/api'
import * as orderService from '../../services/orderService'
import { ROUTES } from '../../utils/constants'
import { useCart } from '../../hooks/useCart'

export function OrderPage({ setNotice, setRoute }) {
  const hasAuthToken = Boolean(getStoredAuth()?.token)
  const [orders, setOrders] = useState([])
  const { loadCart } = useCart()

  useEffect(() => {
    if (!hasAuthToken) return

    loadCart()
    orderService.getOrders()
      .then(setOrders)
      .catch((error) => setNotice(`Chưa đọc được đơn hàng: ${error.message}`))

    const hashQuery = window.location.hash.split('?')[1] || ''
    const params = new URLSearchParams(hashQuery)
    const payment = params.get('payment')
    const orderId = params.get('orderId')
    if (payment === 'success') {
      setNotice(`Thanh toán VNPay thành công cho đơn hàng #${orderId || ''}. Đơn hàng đã được xác nhận và đang chờ Shop chuẩn bị hàng.`)
    } else if (payment === 'failed') {
      setNotice(`Thanh toán chưa thành công cho đơn hàng #${orderId || ''}. Giỏ hàng đã được khôi phục.`)
    }
  }, [hasAuthToken, loadCart, setNotice])

  return (
    <section className="table-panel">
      <h2>Đơn hàng của tôi</h2>
      <DataTable rows={hasAuthToken ? orders : []} columns={['id', 'finalAmount', 'paymentMethod', 'paymentStatus', 'orderStatus']} empty="Chưa có đơn hàng."
        action={(order) => <button type="button" onClick={() => setRoute?.(`${ROUTES.ORDER_DETAIL}?id=${order.id}`)}>{order.orderStatus === 'COMPLETED' ? 'Xem và đánh giá' : order.orderStatus === 'SHIPPING' ? 'Xác nhận nhận hàng' : 'Chi tiết'}</button>} />
    </section>
  )
}
