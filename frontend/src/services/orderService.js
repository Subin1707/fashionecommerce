import { apiRequest } from './api'

export function confirmReceived(id) {
  return apiRequest(`/api/orders/${id}/receive`, { method: 'PATCH' })
}

export function getOrders() {
  return apiRequest('/api/orders')
}

export function getOrder(id) {
  return apiRequest(`/api/orders/${id}`)
}

export function checkout(payload) {
  return apiRequest('/api/checkout', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
