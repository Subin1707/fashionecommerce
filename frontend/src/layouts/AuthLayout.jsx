import { ROUTES } from '../utils/constants'

export function AuthLayout({ title, active, setRoute, children }) {
  return (
    <section className="auth-layout">
      <div className="auth-showcase">
        <img
          className="auth-showcase-image"
          src="https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=760&q=56"
          srcSet="https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=560&q=52 560w, https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=760&q=56 760w, https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=980&q=58 980w"
          sizes="(max-width: 900px) 100vw, 56vw"
          alt=""
          aria-hidden="true"
          fetchPriority="high"
          loading="eager"
          decoding="async"
        />
        <div className="auth-showcase-header">
          <div className="auth-brand"><span>◇</span> FASHION</div>
          <div className="auth-badge">Members club</div>
        </div>

        <div className="auth-showcase-copy">
          <p>Phong cách là ngôn ngữ<br /><em>bạn tự chọn cho mình.</em></p>
          <small>Khám phá bộ sưu tập tinh tế, thời thượng và được tuyển chọn cho những người yêu thời trang.</small>
          <div className="auth-promises">
            <span>Miễn phí vận chuyển</span>
            <span>Đổi trả 30 ngày</span>
            <span>Hàng chính hãng</span>
          </div>
        </div>

        <div className="auth-showcase-footer">
          <div className="mini-stat">
            <strong>24h</strong>
            <span>Giao hàng nhanh</span>
          </div>
          <div className="mini-stat">
            <strong>4.9/5</strong>
            <span>Đánh giá khách hàng</span>
          </div>
        </div>
      </div>

      <div className="auth-card-wrap">
        <div className="auth-card">
          <p className="auth-kicker">Chào mừng trở lại</p>
          <h1>{title}</h1>
          <p className="auth-subtitle">Nơi phong cách riêng của bạn bắt đầu.</p>
          {setRoute && (
            <div className="auth-tabs" aria-label="Auth navigation">
              <button type="button" className={active === ROUTES.LOGIN ? 'active' : ''} onClick={() => setRoute(ROUTES.LOGIN)}>Đăng nhập</button>
              <button type="button" className={active === ROUTES.REGISTER ? 'active' : ''} onClick={() => setRoute(ROUTES.REGISTER)}>Đăng ký</button>
            </div>
          )}
          {children}
          <p className="auth-legal">Bằng việc tiếp tục, bạn đồng ý với điều khoản sử dụng và chính sách bảo mật của FASHION.</p>
        </div>
      </div>
    </section>
  )
}
