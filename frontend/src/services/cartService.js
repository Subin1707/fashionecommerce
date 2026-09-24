import { apiRequest } from './api'

export function getProductVariants(productId) {
  return apiRequest(`/api/products/${productId}/variants`)
}

export function getCart() {
  return apiRequest('/api/cart')
}

export function addCartItem(payload) {
  return apiRequest('/api/cart/items', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateCartItem(id, payload) {
  return apiRequest(`/api/cart/items/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function removeCartItem(id) {
  return apiRequest(`/api/cart/items/${id}`, { method: 'DELETE' })
}
