import { lazy, Suspense, useEffect, useState } from 'react'
import './App.css'
import { AuthProvider } from './context/AuthContext'
import { AppRoutes } from './routes/AppRoutes'
import { getStoredAuth } from './services/api'
import { ROUTES } from './utils/constants'

const CartProvider = lazy(() => import('./context/CartContext').then((module) => ({
  default: module.CartProvider,
})))

const MainLayout = lazy(() => import('./layouts/MainLayout').then((module) => ({
  default: module.MainLayout,
})))

const AUTH_ROUTES = [
  ROUTES.LOGIN,
  ROUTES.REGISTER,
  ROUTES.FORGOT_PASSWORD,
  ROUTES.RESET_PASSWORD,
]

function isAuthRoute(route) {
  return AUTH_ROUTES.includes((route || '').split('?')[0])
}

function getRouteFromHash(fallbackRoute = ROUTES.LOGIN) {
  const currentHash = window.location.hash.replace(/^#/, '').trim()
  if (currentHash) {
    return currentHash
  }

  return getStoredAuth() ? ROUTES.HOME : fallbackRoute
}

function App() {
  const [route, setRouteState] = useState(() => getRouteFromHash())
  const [notice, setNotice] = useState('Đăng nhập hoặc tạo tài khoản để bắt đầu mua sắm.')
  const authRoute = isAuthRoute(route)

  useEffect(() => {
    function syncRouteFromHash() {
      setRouteState(getRouteFromHash())
    }
    window.addEventListener('hashchange', syncRouteFromHash)
    return () => window.removeEventListener('hashchange', syncRouteFromHash)
  }, [])

  // Khi api.js phát hiện 401 từ backend → tự redirect về Login
  useEffect(() => {
    function handleUnauthorized() {
      setNotice('Phiên đăng nhập đã hết hạn hoặc bị từ chối. Vui lòng đăng nhập lại.')
      setRoute(ROUTES.LOGIN)
    }
    window.addEventListener('auth:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized)
  }, [])

  function setRoute(nextRoute) {
    const nextValue = nextRoute || getRouteFromHash()
    window.location.hash = nextValue
    setRouteState(nextValue)
  }

  return (
    <AuthProvider>
      {authRoute ? (
        <main className="auth-shell">
          <AppRoutes route={route} setRoute={setRoute} setNotice={setNotice} />
        </main>
      ) : (
        <Suspense fallback={<div className="route-loading" role="status">Đang tải...</div>}>
          <CartProvider>
            <MainLayout route={route} setRoute={setRoute} notice={notice}>
              <AppRoutes route={route} setRoute={setRoute} setNotice={setNotice} />
            </MainLayout>
          </CartProvider>
        </Suspense>
      )}
    </AuthProvider>
  )
}

export default App
