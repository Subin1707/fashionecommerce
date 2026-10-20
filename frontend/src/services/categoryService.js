import { apiRequest } from './api'
import { repairCategory } from '../utils/repairCatalogText'

export async function getCategories() {
  const data = await apiRequest('/api/categories')
  return Array.isArray(data) ? data.map(normalizeCategory) : data
}

export async function getCategoryById(id) {
  return normalizeCategory(await apiRequest(`/api/categories/${id}`))
}

function normalizeCategory(category) {
  const repaired = repairCategory(category)
  if (!repaired || repaired.parent || repaired.parentId == null) return repaired
  return { ...repaired, parent: { id: repaired.parentId, name: repairCategory({ name: repaired.parentName }).name } }
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
