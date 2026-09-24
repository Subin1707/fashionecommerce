import { apiRequest } from './api'

export const getAllReviews = () => apiRequest('/api/reviews')
export const getMyReviews = () => apiRequest('/api/reviews/mine')
export const getProductReviews = (productId) => apiRequest(`/api/reviews/product/${productId}`)
export const createProductReview = (productId, payload) => apiRequest(`/api/reviews/product/${productId}`, {
  method: 'POST', body: JSON.stringify(payload), timeoutMs: 30000,
})
export const updateReviewStatus = (id, status) => apiRequest(`/api/reviews/${id}/status?${new URLSearchParams({ status })}`, { method: 'PUT' })
export const replyToReview = (id, reply) => apiRequest(`/api/reviews/${id}/reply`, { method: 'PUT', body: JSON.stringify({ reply }) })
export const deleteReview = (id) => apiRequest(`/api/reviews/${id}`, { method: 'DELETE' })
