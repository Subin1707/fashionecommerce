import { apiRequest } from './api'

export function getCategories() {
  return apiRequest('/api/categories')
}

export function getCategoryById(id) {
  return apiRequest(`/api/categories/${id}`)
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
