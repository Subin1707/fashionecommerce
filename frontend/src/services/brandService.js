import { apiRequest } from './api'

export function getBrands() {
  return apiRequest('/api/brands')
}

export function createBrand(payload) {
  return apiRequest('/api/brands', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateBrand(id, payload) {
  return apiRequest(`/api/brands/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteBrand(id) {
  return apiRequest(`/api/brands/${id}`, { method: 'DELETE' })
}
