import { apiRequest } from './api'
import { repairCatalogText } from '../utils/repairCatalogText'

export function repairOrder(order) {
  if (!order) return order
  return {
    ...order,
    shippingAddress: repairCatalogText(order.shippingAddress),
    items: order.items?.map((item) => ({
      ...item,
      productName: repairCatalogText(item.productName),
      color: repairCatalogText(item.color),
      size: repairCatalogText(item.size),
    })),
  }
}

export function confirmReceived(id) {
  return apiRequest(`/api/orders/${id}/receive`, { method: 'PATCH' }).then(repairOrder)
}

export function getOrders() {
  return apiRequest('/api/orders').then((orders) => orders.map(repairOrder))
}

export function getOrder(id) {
  return apiRequest(`/api/orders/${id}`).then(repairOrder)
}

export function getReturnRequests() {
  return apiRequest('/api/orders/return-requests')
}

export function createReturnRequest(orderId, payload, files) {
  const body = new FormData()
  Object.entries(payload).forEach(([key, value]) => body.append(key, String(value)))
  files.forEach((file) => body.append('files', file))
  return apiRequest(`/api/orders/${orderId}/return-requests`, {
    method: 'POST',
    body,
  })
}

export function checkout(payload) {
  return apiRequest('/api/checkout', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
