import { ROUTES } from '../../utils/constants'
import { ProductListPage } from './ProductListPage'

export function HomePage({ setNotice, setRoute }) {
  return (
    <div className="fashion-home">
      {/* HERO */}
      <section className="fashion-hero">
        <div className="fashion-hero-content">
          <p className="fashion-kicker">
            NEW COLLECTION 2026
          </p>

          <h1>
            Phong cách <span className="fashion-hero-accent">mới</span>
            <span> cho phiên bản mới của bạn.</span>
          </h1>

          <p className="fashion-hero-description">
            Khám phá những thiết kế hiện đại, tối giản và dễ phối.
            Mua sắm nhanh chóng với hệ thống sản phẩm, giỏ hàng,
            đơn hàng và gợi ý kích cỡ được đồng bộ trực tiếp.
          </p>

          <div className="fashion-hero-actions">
            <button
              type="button"
              className="fashion-primary-button"
              onClick={() => setRoute(ROUTES.PRODUCTS)}
            >
              Khám phá sản phẩm
              <span>→</span>
            </button>

            <button
              type="button"
              className="fashion-secondary-button"
              onClick={() => setRoute(ROUTES.RECOMMENDATION)}
            >
              Gợi ý kích cỡ
            </button>
          </div>

          <div className="fashion-stats">
            <div>
              <strong>100+</strong>
              <span>Sản phẩm thời trang</span>
            </div>

            <div>
              <strong>7 ngày</strong>
              <span>Hỗ trợ đổi trả</span>
            </div>

            <div>
              <strong>24/7</strong>
              <span>Mua sắm trực tuyến</span>
            </div>
          </div>
        </div>

        <div className="fashion-hero-visual">
          <div className="fashion-hero-card fashion-card-main">
            <div className="fashion-card-placeholder">
              <span>FASHION</span>
              <strong>NEW SEASON</strong>
              <small>Autumn / Winter 2026</small>
            </div>
          </div>

          <div className="fashion-floating-card">
            <span>NEW</span>
            <strong className="fashion-floating-title">
              <span>Phong cách</span>{' '}
              <em>tối giản</em>
            </strong>
            <small>Thanh lịch · Hiện đại · Dễ phối</small>
          </div>
        </div>
      </section>

      {/* SERVICE STRIP */}
      <section className="fashion-benefits">
        <div>
          <span className="fashion-benefit-icon">✦</span>
          <div>
            <strong>Sản phẩm chọn lọc</strong>
            <small>Thiết kế hiện đại và dễ phối</small>
          </div>
        </div>

        <div>
          <span className="fashion-benefit-icon">↗</span>
          <div>
            <strong>Giao hàng nhanh</strong>
            <small>Theo dõi đơn hàng thuận tiện</small>
          </div>
        </div>

        <div>
          <span className="fashion-benefit-icon">↺</span>
          <div>
            <strong>Đổi trả dễ dàng</strong>
            <small>Hỗ trợ đổi trả trong 7 ngày</small>
          </div>
        </div>

        <div>
          <span className="fashion-benefit-icon">✓</span>
          <div>
            <strong>Thanh toán an toàn</strong>
            <small>Bảo mật thông tin giao dịch</small>
          </div>
        </div>
      </section>

      {/* COLLECTION */}
      <section className="fashion-collection-section">
        <div className="fashion-section-heading">
          <div>
            <p className="fashion-kicker">
              KHÁM PHÁ PHONG CÁCH
            </p>

            <h2>
              Bộ sưu tập dành cho bạn
            </h2>
          </div>

          <button
            type="button"
            className="fashion-text-button"
            onClick={() => setRoute(ROUTES.PRODUCTS)}
          >
            Xem tất cả
            <span>→</span>
          </button>
        </div>

        <div className="fashion-collection-grid">
          <button
            type="button"
            className="fashion-collection-card fashion-collection-large"
            onClick={() => setRoute(ROUTES.PRODUCTS)}
          >
            <div className="fashion-collection-background collection-one" />

            <div className="fashion-collection-content">
              <small>COLLECTION 01</small>

              <h3>
                Modern
                <br />
                Essentials
              </h3>

              <span>
                Khám phá →
              </span>
            </div>
          </button>

          <button
            type="button"
            className="fashion-collection-card"
            onClick={() => setRoute(ROUTES.PRODUCTS)}
          >
            <div className="fashion-collection-background collection-two" />

            <div className="fashion-collection-content">
              <small>COLLECTION 02</small>

              <h3>
                Everyday
                <br />
                Style
              </h3>

              <span>
                Khám phá →
              </span>
            </div>
          </button>

          <button
            type="button"
            className="fashion-collection-card"
            onClick={() => setRoute(ROUTES.PRODUCTS)}
          >
            <div className="fashion-collection-background collection-three" />

            <div className="fashion-collection-content">
              <small>COLLECTION 03</small>

              <h3>
                Minimal
                <br />
                Wardrobe
              </h3>

              <span>
                Khám phá →
              </span>
            </div>
          </button>
        </div>
      </section>

      {/* PRODUCT LIST FROM YOUR API */}
      <section className="fashion-products-section">
        <div className="fashion-section-heading">
          <div>
            <p className="fashion-kicker">
              SẢN PHẨM
            </p>

            <h2>
              Khám phá sản phẩm mới
            </h2>

            <p className="fashion-section-description">
              Những sản phẩm đang có trên cửa hàng của bạn.
            </p>
          </div>
        </div>

        <div className="fashion-product-list-wrapper">
          <ProductListPage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </div>
      </section>

      {/* SIZE RECOMMENDATION CTA */}
      <section className="fashion-size-banner">
        <div className="fashion-size-decoration" />

        <div className="fashion-size-content">
          <p className="fashion-kicker light">
            SMART FASHION
          </p>

          <h2>
            Không chắc bạn nên chọn size nào?
          </h2>

          <p>
            Sử dụng tính năng gợi ý kích cỡ để tìm size phù hợp hơn
            trước khi đặt hàng.
          </p>
        </div>

        <button
          type="button"
          className="fashion-size-button"
          onClick={() => setRoute(ROUTES.RECOMMENDATION)}
        >
          Gợi ý kích cỡ
          <span>→</span>
        </button>
      </section>
    </div>
  )
}
