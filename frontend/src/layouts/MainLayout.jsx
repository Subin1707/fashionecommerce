import { ADMIN_NAV, CUSTOMER_NAV, ROUTES } from '../utils/constants'
import { useAuth } from '../hooks/useAuth'
import { useCart } from '../hooks/useCart'

export function MainLayout({ route, setRoute, notice, children }) {
  const { auth, isAdmin, logout } = useAuth()
  const { itemCount } = useCart()

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
            <span className="guest-brand-logo">
              F
            </span>

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
              onClick={() => setRoute(ROUTES.HOME)}
            >
              Trang chủ
            </button>

            <button
              type="button"
              className={route === ROUTES.PRODUCTS ? 'active' : ''}
              onClick={() => setRoute(ROUTES.PRODUCTS)}
            >
              Sản phẩm
            </button>

            <button
              type="button"
              onClick={() => setRoute(ROUTES.PRODUCTS)}
            >
              Bộ sưu tập
            </button>

            <button
              type="button"
              onClick={() => setRoute(ROUTES.HOME)}
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
          <div>
            <strong>FASHION</strong>
            <span>Thời trang dành cho phong cách của bạn.</span>
          </div>

          <p>© 2026 Fashion Store</p>
        </footer>
      </main>
    )
  }

  // ======================================================
  // ĐÃ ĐĂNG NHẬP
  // ======================================================

  const navItems = isAdmin ? ADMIN_NAV : CUSTOMER_NAV

  return (
    <main className="app-shell">
      <aside className="side-nav">
        <div className="brand-mark">
          <span>F</span>

          <div>
            <strong>Thời trang cao cấp</strong>
            <small>Không gian mua sắm trực tuyến</small>
          </div>
        </div>

        <nav>
          {navItems.map(([label, value]) => (
            <button
              key={value}
              className={route === value ? 'active' : ''}
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
          onLogout={logout}
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
    </main>
  )
}

function SessionPanel({ auth, onLogout }) {
  return (
    <div className="login-panel">
      <div>
        <strong>{auth.email}</strong>
        <small>{auth.role}</small>
      </div>

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