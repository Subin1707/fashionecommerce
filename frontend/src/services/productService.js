import { apiRequest, pageContent } from './api'
import { repairProduct } from '../utils/repairCatalogText'

export async function searchProducts(filters = {}, { page = 0, size = 16 } = {}) {
  const params = new URLSearchParams({ page: String(page), size_param: String(size) })
  if (filters.keyword) params.set('keyword', filters.keyword)
  if (filters.category) params.set('category', filters.category)
  if (filters.size) params.set('size', filters.size)
  if (filters.color) params.set('color', filters.color)
  const minPrice = parsePrice(filters.minPrice)
  const maxPrice = parsePrice(filters.maxPrice)
  if (minPrice != null) params.set('minPrice', String(minPrice))
  if (maxPrice != null) params.set('maxPrice', String(maxPrice))

  const data = await apiRequest(`/api/search?${params}`)
  const products = pageContent(data).map(repairProduct)
  return {
    products,
    totalPages: Array.isArray(data) ? 1 : Math.max(1, Number(data?.totalPages) || 1),
  }
}

function parsePrice(value) {
  if (value == null || String(value).trim() === '') return null
  const normalized = String(value).trim().replace(/\s/g, '')
  const price = Number(normalized.replace(/[,.](?=\d{3}(?:\D|$))/g, '').replace(',', '.'))
  return Number.isFinite(price) && price >= 0 ? price : null
}

export async function getProduct(id) {
  return repairProduct(await apiRequest(`/api/products/${id}`))
}

export function getProductVariants(productId) {
  return apiRequest(`/api/products/${productId}/variants`)
}

export async function getSimilarProducts(productId, limit = 8) {
  const data = await apiRequest(`/api/products/${productId}/similar?limit=${limit}`)
  return Array.isArray(data) ? data.map(repairProduct) : data
}
