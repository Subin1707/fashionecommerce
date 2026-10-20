import { ADMIN_NAV, CUSTOMER_NAV, ROUTES } from '../utils/constants'
import { useAuth } from '../hooks/useAuth'
import { useCart } from '../hooks/useCart'
import { useEffect, useState } from 'react'
import * as userService from '../services/userService'
import { FashionMark } from '../components/common/FashionMark'
import { StyleChatWidget } from '../components/common/StyleChatWidget'

export function MainLayout({ route, setRoute, notice, children }) {
  const { auth, isAdmin, logout } = useAuth()
  const { itemCount } = useCart()
  const [profileName, setProfileName] = useState('')

  useEffect(() => {
    if (!auth || auth.role !== 'CUSTOMER') {
      setProfileName('')
      return undefined
    }
    let cancelled = false
    userService.getProfile()
      .then((profile) => {
        if (!cancelled) setProfileName(profile?.fullName?.trim() || '')
      })
      .catch(() => {
        if (!cancelled) setProfileName('')
      })
    return () => { cancelled = true }
  }, [auth?.email, auth?.role])

  const isAuthRoute = [
    'login',
    'register',
    'forgot-password',
    'reset-password',
  ].includes(route)

  // Các trang login / register vẫn dùng giao diện auth riêng
  if (isAuthRoute) {
    return (
      <main className="auth-shell">
        {children}
      </main>
    )
  }

  // ======================================================
  // CHƯA ĐĂNG NHẬP - GIAO DIỆN KHÁCH
  // ======================================================

  if (!auth) {
    return (
      <main className="guest-layout">
        <header className="guest-header">
          {/* LOGO */}
          <button
            type="button"
            className="guest-brand"
            onClick={() => setRoute(ROUTES.HOME)}
          >
            <FashionMark className="guest-brand-logo" />

            <span className="guest-brand-info">
              <strong>FASHION</strong>
              <small>PREMIUM STORE</small>
            </span>
          </button>

          {/* MENU */}
          <nav className="guest-navigation">
            <button
              type="button"
              className={route === ROUTES.HOME ? 'active' : ''}
              aria-current={route === ROUTES.HOME ? 'page' : undefined}
              onClick={() => setRoute(ROUTES.HOME)}
            >
              Trang chủ
            </button>

            <button
              type="button"
              className={route === ROUTES.PRODUCTS ? 'active' : ''}
              aria-current={route === ROUTES.PRODUCTS ? 'page' : undefined}
              onClick={() => setRoute(ROUTES.PRODUCTS)}
            >
              Sản phẩm
            </button>

            <button
              type="button"
              className={route === `${ROUTES.HOME}?section=collections` ? 'active' : ''}
              aria-current={route === `${ROUTES.HOME}?section=collections` ? 'location' : undefined}
              onClick={() => setRoute(`${ROUTES.HOME}?section=collections`)}
            >
              Bộ sưu tập
            </button>

            <button
              type="button"
              className={route === `${ROUTES.HOME}?section=brands` ? 'active' : ''}
              aria-current={route === `${ROUTES.HOME}?section=brands` ? 'location' : undefined}
              onClick={() => setRoute(`${ROUTES.HOME}?section=brands`)}
            >
              Thương hiệu
            </button>
          </nav>

          {/* ACTION */}
          <div className="guest-header-actions">
            <button
              type="button"
              className="guest-sign-in"
              onClick={() => setRoute(ROUTES.LOGIN)}
            >
              Đăng nhập
            </button>

            <button
              type="button"
              className="guest-sign-up"
              onClick={() => setRoute(ROUTES.REGISTER)}
            >
              Đăng ký
            </button>
          </div>
        </header>

        {notice && (
          <div className="guest-notice">
            {notice}
          </div>
        )}

        <section className="guest-main">
          {children}
        </section>

        <footer className="guest-footer">
          <div className="guest-footer-main">
            <section className="guest-footer-brand" aria-label="Giới thiệu FASHION">
              <div className="guest-footer-brand-lockup">
                <FashionMark className="guest-footer-mark" />
                <div>
                  <strong>FASHION</strong>
                  <small>PREMIUM STORE</small>
                </div>
              </div>
              <p>
                Thời trang dành cho phong cách của bạn. Khám phá những lựa chọn
                giúp bạn tự tin thể hiện chất riêng mỗi ngày.
              </p>
              <div className="guest-footer-tags" aria-label="Phong cách">
                <span>Tinh tế</span>
                <span>Hiện đại</span>
                <span>Dễ phối</span>
              </div>
            </section>

            <nav className="guest-footer-links" aria-label="Khám phá cửa hàng">
              <h2>Khám phá</h2>
              <button type="button" onClick={() => setRoute(ROUTES.HOME)}>
                Trang chủ
              </button>
              <button type="button" onClick={() => setRoute(ROUTES.PRODUCTS)}>
                Tất cả sản phẩm
              </button>
              <button type="button" onClick={() => setRoute(`${ROUTES.HOME}?section=collections`)}>
                Bộ sưu tập
              </button>
            </nav>

            <nav className="guest-footer-links" aria-label="Tài khoản">
              <h2>Tài khoản</h2>
              <button type="button" onClick={() => setRoute(ROUTES.LOGIN)}>
                Đăng nhập
              </button>
              <button type="button" onClick={() => setRoute(ROUTES.REGISTER)}>
                Tạo tài khoản
              </button>
              <p className="guest-footer-note">
                Đăng nhập để tiếp tục quản lý trải nghiệm mua sắm của bạn.
              </p>
            </nav>

            <section className="guest-footer-cta">
              <span className="guest-footer-cta-label">BẮT ĐẦU KHÁM PHÁ</span>
              <h2>Tìm món đồ dành riêng cho bạn</h2>
              <p>Dạo quanh cửa hàng và tìm cảm hứng cho phong cách mới.</p>
              <button type="button" onClick={() => setRoute(ROUTES.PRODUCTS)}>
                Khám phá sản phẩm <span aria-hidden="true">→</span>
              </button>
            </section>
          </div>

          <div className="guest-footer-bottom">
            <p>© {new Date().getFullYear()} Fashion Store. Bảo lưu mọi quyền.</p>
            <button type="button" onClick={() => setRoute(ROUTES.HOME)}>
              Về đầu trang <span aria-hidden="true">↑</span>
            </button>
          </div>
        </footer>
      </main>
    )
  }

  // ======================================================
  // ĐÃ ĐĂNG NHẬP
  // ======================================================

  const navItems = isAdmin ? ADMIN_NAV : CUSTOMER_NAV

  return (
    <main className={`app-shell ${isAdmin ? 'admin-shell' : 'customer-shell'}`}>
      <aside className="side-nav">
        <div className="brand-mark">
          <FashionMark className="brand-mark-icon" />

          <div>
            <strong>Thời trang cao cấp</strong>
            <small>Không gian mua sắm trực tuyến</small>
          </div>
        </div>

        <nav aria-label={isAdmin ? 'Điều hướng quản trị' : 'Điều hướng khách hàng'}>
          {navItems.map(([label, value]) => (
            <button
              key={value}
              className={route === value ? 'active' : ''}
              aria-current={route === value ? 'page' : undefined}
              onClick={() => setRoute(value)}
            >
              <span>{label}</span>

              {value === ROUTES.CART && itemCount > 0 && (
                <span className="cart-nav-badge">
                  {itemCount}
                </span>
              )}
            </button>
          ))}
        </nav>

        <SessionPanel
          auth={auth}
          displayName={profileName}
          onLogout={logout}
          itemCount={itemCount}
          onOpenCart={() => setRoute(ROUTES.CART)}
          showCart={!isAdmin && auth.role === 'CUSTOMER'}
          currentRoute={route}
        />
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">
              Hệ thống đang đồng bộ
            </p>

            <h1>
              {isAdmin && route.startsWith('admin')
                ? 'Quản trị vận hành'
                : 'Cửa hàng thời trang'}
            </h1>
          </div>

          <div className="session-pill">
            {`${auth.role} - ${auth.email}`}
          </div>
        </header>

        {notice && (
          <p className="notice">
            {notice}
          </p>
        )}

        {children}
      </section>
      {!isAdmin && auth.role === 'CUSTOMER' && <StyleChatWidget setRoute={setRoute} route={route} />}
    </main>
  )
}

function SessionPanel({ auth, displayName, onLogout, itemCount, onOpenCart, showCart, currentRoute }) {
  return (
    <div className="login-panel">
      <div>
        <strong>{displayName || auth.fullName || auth.email}</strong>
        <small>{auth.role}</small>
      </div>

      {showCart && (
        <button
          type="button"
          className={`header-cart-button ${currentRoute === ROUTES.CART ? 'active' : ''}`}
          aria-label={`Giỏ hàng, ${itemCount} sản phẩm`}
          title="Giỏ hàng"
          onClick={onOpenCart}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none">
            <path d="M3 4h2l2.1 10.1a2 2 0 0 0 2 1.6h8.8a2 2 0 0 0 1.9-1.4L22 8H6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            <circle cx="10" cy="20" r="1.3" fill="currentColor" />
            <circle cx="18" cy="20" r="1.3" fill="currentColor" />
          </svg>
          {itemCount > 0 && <span className="header-cart-count">{itemCount > 99 ? '99+' : itemCount}</span>}
        </button>
      )}

      <button
        type="button"
        className="ghost"
        onClick={onLogout}
      >
        Đăng xuất
      </button>
    </div>
  )
}
