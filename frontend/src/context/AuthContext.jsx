import { useEffect, useMemo, useState } from 'react'
import { AuthContext } from './authContextStore'
import { clearStoredAuth, getStoredAuth } from '../services/api'
import * as authService from '../services/authService'

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(getStoredAuth)

  // Lắng nghe sự kiện từ api.js khi backend trả về 401
  // → tự động đăng xuất và làm sạch token hỏng
  useEffect(() => {
    function handleUnauthorized() {
      clearStoredAuth()
      setAuth(null)
    }
    window.addEventListener('auth:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized)
  }, [])

  async function login(credentials) {
    const nextAuth = await authService.login(credentials)
    setAuth(nextAuth)
    return nextAuth
  }

  function logout() {
    authService.logout()
    setAuth(null)
  }

  const value = useMemo(() => ({
    auth,
    isAuthenticated: Boolean(auth?.token),
    isAdmin: auth?.role === 'ADMIN',
    isCustomer: auth?.role === 'CUSTOMER',
    login,
    logout,
  }), [auth])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
