import { apiRequest, pageContent } from './api'

export async function searchProducts(filters = {}) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value)
  })
  params.set('size_param', filters.size_param || '12')
  const data = await apiRequest(`/api/search?${params}`)
  return pageContent(data)
}

export function getProduct(id) {
  return apiRequest(`/api/products/${id}`)
}

export function getProductVariants(productId) {
  return apiRequest(`/api/products/${productId}/variants`)
}

export function getSimilarProducts(productId, limit = 8) {
  return apiRequest(`/api/products/${productId}/similar?limit=${limit}`)
}
