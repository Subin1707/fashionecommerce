import { apiRequest } from './api'
import { repairCategory } from '../utils/repairCatalogText'

export async function getCategories() {
  const data = await apiRequest('/api/categories')
  return Array.isArray(data) ? data.map(repairCategory) : data
}

export async function getCategoryById(id) {
  return repairCategory(await apiRequest(`/api/categories/${id}`))
}

export function createCategory(payload) {
  return apiRequest('/api/categories', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateCategory(id, payload) {
  return apiRequest(`/api/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteCategory(id) {
  return apiRequest(`/api/categories/${id}`, { method: 'DELETE' })
}
