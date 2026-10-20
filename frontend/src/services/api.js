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
  const timeoutId = setTimeout(() => controller.abort(), options.timeoutMs || 15000)
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
      if (options.signal?.aborted) throw error
      throw new Error('Máy chủ phản hồi quá lâu. Vui lòng kiểm tra kết nối hoặc thử tải lại trang.')
    }
    throw error
  } finally {
    clearTimeout(timeoutId)
  }

  const text = await response.text()
  const data = parseResponseBody(text)

  if (!response.ok) {
    if (response.status === 401) {
      if (path === '/api/auth/login') {
        throw new Error(data?.message || 'Thông tin đăng nhập không chính xác. Vui lòng kiểm tra lại.')
      }
      clearStoredAuth()
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
      throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
    }

    if (response.status === 403) {
      if (path === '/api/auth/login') {
        throw new Error(data?.message || 'Yêu cầu đăng nhập bị máy chủ từ chối. Vui lòng kiểm tra email, mật khẩu và trạng thái tài khoản.')
      }
      throwForbiddenError(path)
    }

    const serverMessage = data?.message || data?.error || text
    if (response.status === 404 && String(serverMessage).startsWith('Resource not found')) {
      const resourcePath = String(serverMessage).match(/Resource not found:\s*(.+)$/)?.[1]
      const wrongResource = resourcePath ? `Backend báo không có tài nguyên ${resourcePath}.` : ''
      throw new Error(`API không tìm thấy ${options.method || 'GET'} ${path} (HTTP 404). ${wrongResource} Hãy dừng backend cũ và chạy .\\scripts\\run-backend-current.ps1 từ thư mục E:\\fashionecommerce.`)
    }
    throw new Error(serverMessage || `Lỗi máy chủ (HTTP ${response.status})`)
  }

  return data
}

function throwForbiddenError(path) {
  const auth = getStoredAuth()

  if (!auth?.token) {
    window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    throw new Error('Bạn chưa đăng nhập. Vui lòng đăng nhập lại.')
  }

  if (path.startsWith('/api/admin')) {
    throw new Error('Phiên quản trị không hợp lệ. Vui lòng đăng xuất và đăng nhập lại bằng tài khoản quản trị.')
  }

  if (path.startsWith('/api/cart') && auth.role === 'CUSTOMER') {
    clearStoredAuth()
    window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    throw new Error('Phiên khách hàng không hợp lệ. Vui lòng đăng nhập lại bằng tài khoản khách hàng.')
  }

  if (auth.role === 'ADMIN') {
    throw new Error('Tài khoản quản trị không có quyền thực hiện thao tác dành cho khách hàng. Vui lòng dùng tài khoản khách hàng.')
  }

  throw new Error('Bạn không có quyền truy cập. Vui lòng kiểm tra tài khoản đang đăng nhập.')
}

function parseResponseBody(text) {
  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}
