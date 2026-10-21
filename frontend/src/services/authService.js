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

export function verifyOtp(payload) {
  return apiRequest('/api/auth/verify-otp', {
    method: 'POST',
    body: JSON.stringify(payload),
  }).then((result) => ({
    ...result,
    // Older backend versions return { message, email, role } on successful verification.
    verified: result?.verified ?? Boolean(result?.email),
  }))
}

export function requestPasswordReset(email) {
  return apiRequest('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function resetPassword(payload) {
  return apiRequest('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
