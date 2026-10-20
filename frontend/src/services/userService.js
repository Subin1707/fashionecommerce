import { apiRequest } from './api'

export function getUsers() {
  return apiRequest('/api/users')
}

export function getProfile() {
  return apiRequest('/api/users/me')
}

export function updateProfile(payload) {
  return apiRequest('/api/users/me', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function uploadAvatar(file) {
  const body = new FormData()
  body.append('file', file)
  return apiRequest('/api/users/me/avatar', { method: 'POST', body })
}

export function updateUser(id, payload) {
  return apiRequest(`/api/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
