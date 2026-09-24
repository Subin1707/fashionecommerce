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

export function updateUser(id, payload) {
  return apiRequest(`/api/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
