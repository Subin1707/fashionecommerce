import { apiRequest } from './api'

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

export function campaignImageSource(url) {
  if (url?.startsWith('/uploads/events/')) return `${API_BASE}${url}`
  return url
}

export function getActiveCampaignEvents() {
  return apiRequest('/api/events/active')
}

export function getAdminCampaignEvents() {
  return apiRequest('/api/admin/events')
}

export function createCampaignEvent(payload) {
  return apiRequest('/api/admin/events', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateCampaignEvent(id, payload) {
  return apiRequest(`/api/admin/events/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteCampaignEvent(id) {
  return apiRequest(`/api/admin/events/${id}`, { method: 'DELETE' })
}

export function uploadCampaignImage(file) {
  const body = new FormData()
  body.append('file', file)
  return apiRequest('/api/admin/events/images', { method: 'POST', body })
}
