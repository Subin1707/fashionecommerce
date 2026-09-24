import { apiRequest } from './api'

export function getWishlist() {
  return apiRequest('/api/wishlist')
}

export function addToWishlist(productId) {
  return apiRequest(`/api/wishlist/${productId}`, { method: 'POST' })
}

export function removeFromWishlist(productId) {
  return apiRequest(`/api/wishlist/${productId}`, { method: 'DELETE' })
}
