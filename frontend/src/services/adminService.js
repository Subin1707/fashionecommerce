import { apiRequest, pageContent } from './api'

export function getDashboard() {
  return apiRequest('/api/admin/dashboard')
}

export async function getAdminProducts({ page = 0, size = 100 } = {}) {
  return pageContent(await apiRequest(`/api/admin/products?page=${page}&size=${size}`))
}

export function createAdminProduct(payload) {
  return apiRequest('/api/admin/products', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateAdminProduct(productId, payload) {
  return apiRequest(`/api/admin/products/${productId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteAdminProduct(productId) {
  return apiRequest(`/api/admin/products/${productId}`, { method: 'DELETE' })
}

export async function getAdminOrders({ page = 0, size = 100 } = {}) {
  return pageContent(await apiRequest(`/api/admin/orders?page=${page}&size=${size}`))
}

export async function getAdminOrdersByStatus(status, { page = 0, size = 100 } = {}) {
  return pageContent(await apiRequest(`/api/admin/orders/status/${status}?page=${page}&size=${size}`))
}

export function getAdminOrder(orderId) {
  return apiRequest(`/api/admin/orders/${orderId}`)
}

export function updateAdminOrderStatus(orderId, orderStatus) {
  return apiRequest(`/api/admin/orders/${orderId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ orderStatus }),
  })
}

export function getCustomers() {
  return apiRequest('/api/admin/customers')
}

export function lockCustomer(id) {
  return apiRequest(`/api/admin/customers/${id}/lock`, { method: 'PUT' })
}

export function unlockCustomer(id) {
  return apiRequest(`/api/admin/customers/${id}/unlock`, { method: 'PUT' })
}

export function getAdminProductVariants(productId) {
  return apiRequest(`/api/admin/products/${productId}/variants`)
}

export function createAdminProductVariant(productId, payload) {
  return apiRequest(`/api/admin/products/${productId}/variants`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateAdminProductVariant(variantId, payload) {
  return apiRequest(`/api/admin/products/variants/${variantId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteAdminProductVariant(variantId) {
  return apiRequest(`/api/admin/products/variants/${variantId}`, {
    method: 'DELETE',
  })
}

