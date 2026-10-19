import { apiRequest } from './api'
import { repairBrand } from '../utils/repairCatalogText'

export async function getBrands() {
  const data = await apiRequest('/api/brands')
  return Array.isArray(data) ? data.map(repairBrand) : data
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
