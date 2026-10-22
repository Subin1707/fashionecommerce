import { apiRequest } from './api'
import { repairCategory } from '../utils/repairCatalogText'

export async function getCategories() {
  const data = await apiRequest('/api/categories')
  return Array.isArray(data) ? data.map(normalizeCategory) : data
}

export async function getCategoriesWithProducts() {
  const data = await apiRequest('/api/categories/with-products')
  return Array.isArray(data) ? data.map(normalizeCategory) : data
}

export async function getCategoryById(id) {
  return normalizeCategory(await apiRequest(`/api/categories/${id}`))
}

function normalizeCategory(category) {
  const repaired = repairCategory(category)
  if (!repaired) return repaired

  const parentId = repaired.parent?.id ?? repaired.parentId
  if (parentId == null) return { ...repaired, parent: null }

  const parentName = repaired.parent?.name ?? repaired.parentName
  return {
    ...repaired,
    parent: repairCategory({ ...(repaired.parent || {}), id: parentId, name: parentName }),
  }
}

export function createCategory(payload) {
  return apiRequest('/api/categories', {
    method: 'POST',
    body: JSON.stringify(payload),
  }).then(normalizeCategory)
}

export function updateCategory(id, payload) {
  return apiRequest(`/api/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }).then(normalizeCategory)
}

export function deleteCategory(id) {
  return apiRequest(`/api/categories/${id}`, { method: 'DELETE' })
}
