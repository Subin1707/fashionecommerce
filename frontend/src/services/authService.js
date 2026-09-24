import { apiRequest, clearStoredAuth, saveStoredAuth } from './api'

export async function login(credentials) {
  const auth = await apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
  saveStoredAuth(auth)
  return auth
}

export function logout() {
  clearStoredAuth()
}

export function register(payload) {
  return apiRequest('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
