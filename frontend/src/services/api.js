const API_BASE = import.meta.env.VITE_API_BASE_URL || ''
const AUTH_KEY = 'fashion.auth'

export function getStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY)) || null
  } catch {
    return null
  }
}

export function saveStoredAuth(auth) {
  localStorage.setItem(AUTH_KEY, JSON.stringify(auth))
}

export function clearStoredAuth() {
  localStorage.removeItem(AUTH_KEY)
}

export function pageContent(payload) {
  return Array.isArray(payload) ? payload : payload?.content || []
}

export async function apiRequest(path, options = {}) {
  const auth = getStoredAuth()
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), options.timeoutMs || 10000)
  let response

  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      signal: options.signal || controller.signal,
      headers: {
        Accept: 'application/json',
        ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...(auth?.token ? { Authorization: `Bearer ${auth.token}` } : {}),
        ...options.headers,
      },
    })
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error('API phan hoi qua lau, vui long kiem tra backend hoac thu tai lai.')
    }
    throw error
  } finally {
    clearTimeout(timeoutId)
  }

  const text = await response.text()
  const data = parseResponseBody(text)

  if (!response.ok) {
    if (response.status === 401) {
      clearStoredAuth()
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
      throw new Error('Phien dang nhap da het han. Vui long dang nhap lai.')
    }

    if (response.status === 403) {
      throwForbiddenError(path)
    }

    throw new Error(data?.message || data?.error || text || `Loi may chu (HTTP ${response.status})`)
  }

  return data
}

function throwForbiddenError(path) {
  const auth = getStoredAuth()

  if (!auth?.token) {
    window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    throw new Error('Ban chua dang nhap. Vui long dang nhap lai.')
  }

  if (path.startsWith('/api/admin')) {
    throw new Error('Phien Admin khong duoc backend chap nhan. Vui long dang xuat, dang nhap lai bang tai khoan Admin roi thu lai.')
  }

  if (path.startsWith('/api/cart') && auth.role === 'CUSTOMER') {
    clearStoredAuth()
    window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    throw new Error('Phien Customer khong duoc backend chap nhan. Vui long dang nhap lai bang tai khoan Customer.')
  }

  if (auth.role === 'ADMIN') {
    throw new Error('Tai khoan Admin khong co quyen thuc hien thao tac danh cho Khach hang. Vui long dung tai khoan Customer.')
  }

  throw new Error('Khong co quyen truy cap. Vui long kiem tra tai khoan dang nhap.')
}

function parseResponseBody(text) {
  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}
