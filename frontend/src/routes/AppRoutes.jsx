import { lazy, Suspense } from 'react'
import { AdminLayout } from '../layouts/AdminLayout'
import { AdminRoute } from './AdminRoute'
import { PrivateRoute } from './PrivateRoute'
import { ROUTES } from '../utils/constants'

const HomePage = lazyPage(() => import('../pages/customer/HomePage'), 'HomePage')
const ProductListPage = lazyPage(() => import('../pages/customer/ProductListPage'), 'ProductListPage')
const ProductDetailPage = lazyPage(() => import('../pages/customer/ProductDetailPage'), 'ProductDetailPage')
const CartPage = lazyPage(() => import('../pages/customer/CartPage'), 'CartPage')
const CheckoutPage = lazyPage(() => import('../pages/customer/CheckoutPage'), 'CheckoutPage')
const OrderPage = lazyPage(() => import('../pages/customer/OrderPage'), 'OrderPage')
const OrderDetailPage = lazyPage(() => import('../pages/customer/OrderDetailPage'), 'OrderDetailPage')
const ProfilePage = lazyPage(() => import('../pages/customer/ProfilePage'), 'ProfilePage')
const ReviewPage = lazyPage(() => import('../pages/customer/ReviewPage'), 'ReviewPage')
const WishlistPage = lazyPage(() => import('../pages/customer/WishlistPage'), 'WishlistPage')
const RecommendationPage = lazyPage(() => import('../pages/customer/RecommendationPage'), 'RecommendationPage')

const LoginPage = lazyPage(() => import('../pages/auth/LoginPage'), 'LoginPage')
const RegisterPage = lazyPage(() => import('../pages/auth/RegisterPage'), 'RegisterPage')
const ForgotPasswordPage = lazyPage(() => import('../pages/auth/ForgotPasswordPage'), 'ForgotPasswordPage')
const ResetPasswordPage = lazyPage(() => import('../pages/auth/ResetPasswordPage'), 'ResetPasswordPage')

const DashboardPage = lazyPage(() => import('../pages/admin/DashboardPage'), 'DashboardPage')
const ProductManagementPage = lazyPage(() => import('../pages/admin/ProductManagementPage'), 'ProductManagementPage')
const ProductFormPage = lazyPage(() => import('../pages/admin/ProductFormPage'), 'ProductFormPage')
const InventoryPage = lazyPage(() => import('../pages/admin/InventoryPage'), 'InventoryPage')
const OrderManagementPage = lazyPage(() => import('../pages/admin/OrderManagementPage'), 'OrderManagementPage')
const CustomerManagementPage = lazyPage(() => import('../pages/admin/CustomerManagementPage'), 'CustomerManagementPage')
const UserManagementPage = lazyPage(() => import('../pages/admin/UserManagementPage'), 'UserManagementPage')
const CategoryManagementPage = lazyPage(() => import('../pages/admin/CategoryManagementPage'), 'CategoryManagementPage')
const BrandManagementPage = lazyPage(() => import('../pages/admin/BrandManagementPage'), 'BrandManagementPage')
const DiscountManagementPage = lazyPage(() => import('../pages/admin/DiscountManagementPage'), 'DiscountManagementPage')
const ReviewManagementPage = lazyPage(() => import('../pages/admin/ReviewManagementPage'), 'ReviewManagementPage')
const ReportPage = lazyPage(() => import('../pages/admin/ReportPage'), 'ReportPage')

function lazyPage(loader, exportName) {
  return lazy(() => loader().then((module) => ({ default: module[exportName] })))
}

function RouteFallback() {
  return <div className="route-loading" role="status">Đang tải...</div>
}

export function AppRoutes({ route, setRoute, setNotice }) {
  const props = {
    setNotice,
    setRoute,
  }

  const currentRoute = route || ROUTES.HOME
  const routeName = currentRoute.split('?')[0]

  let page

  switch (routeName) {
    // =========================
    // CUSTOMER
    // =========================

    case ROUTES.HOME:
      page = (
        <HomePage
          {...props}
        />
      )
      break

    case ROUTES.PRODUCTS:
      page = (
        <ProductListPage
          setNotice={setNotice}
          setRoute={setRoute}
        />
      )
      break

    case ROUTES.PRODUCT_DETAIL:
      page = (
        <ProductDetailPage
          setNotice={setNotice}
          setRoute={setRoute}
        />
      )
      break

    // =========================
    // CART
    // =========================

    case ROUTES.CART:
      page = (
        <PrivateRoute>
          <CartPage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </PrivateRoute>
      )
      break

    // =========================
    // CHECKOUT
    // =========================

    case ROUTES.CHECKOUT:
      page = (
        <PrivateRoute>
          <CheckoutPage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </PrivateRoute>
      )
      break

    // =========================
    // ORDERS
    // =========================

    case ROUTES.ORDERS:
      page = (
        <PrivateRoute>
          <OrderPage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </PrivateRoute>
      )
      break

    case ROUTES.ORDER_DETAIL:
      page = (
        <PrivateRoute>
          <OrderDetailPage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </PrivateRoute>
      )
      break

    // =========================
    // PROFILE
    // =========================

    case ROUTES.PROFILE:
      page = (
        <PrivateRoute>
          <ProfilePage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </PrivateRoute>
      )
      break

    // =========================
    // REVIEW
    // =========================

    case ROUTES.REVIEW:
      page = (
        <PrivateRoute>
          <ReviewPage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </PrivateRoute>
      )
      break

    // =========================
    // WISHLIST
    // =========================

    case ROUTES.WISHLIST:
      page = (
        <PrivateRoute>
          <WishlistPage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </PrivateRoute>
      )
      break

    // =========================
    // RECOMMENDATION
    // =========================

    case ROUTES.RECOMMENDATION:
      page = (
        <PrivateRoute>
          <RecommendationPage
            setNotice={setNotice}
            setRoute={setRoute}
          />
        </PrivateRoute>
      )
      break

    // =========================
    // AUTH
    // =========================

    case ROUTES.LOGIN:
      page = (
        <LoginPage
          setNotice={setNotice}
          setRoute={setRoute}
        />
      )
      break

    case ROUTES.REGISTER:
      page = (
        <RegisterPage
          setNotice={setNotice}
          setRoute={setRoute}
        />
      )
      break

    case ROUTES.FORGOT_PASSWORD:
      page = (
        <ForgotPasswordPage
          setNotice={setNotice}
          setRoute={setRoute}
        />
      )
      break

    case ROUTES.RESET_PASSWORD:
      page = (
        <ResetPasswordPage
          setNotice={setNotice}
          setRoute={setRoute}
        />
      )
      break

    // =========================
    // ADMIN
    // =========================

    default:
      page = (
        <AdminRoutes
          route={route}
          setRoute={setRoute}
          setNotice={setNotice}
        />
      )
  }

  return <Suspense fallback={<RouteFallback />}>{page}</Suspense>
}

function AdminRoutes({
  route,
  setRoute,
  setNotice,
}) {
  let page = (
    <DashboardPage
      setNotice={setNotice}
    />
  )

  if (route === ROUTES.ADMIN_PRODUCTS) {
    page = (
      <ProductManagementPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_PRODUCT_FORM) {
    page = (
      <ProductFormPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_INVENTORY) {
    page = (
      <InventoryPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_ORDERS) {
    page = (
      <OrderManagementPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_CUSTOMERS) {
    page = (
      <CustomerManagementPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_USERS) {
    page = (
      <UserManagementPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_CATEGORIES) {
    page = (
      <CategoryManagementPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_BRANDS) {
    page = (
      <BrandManagementPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_DISCOUNTS) {
    page = (
      <DiscountManagementPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_REVIEWS) {
    page = (
      <ReviewManagementPage
        setNotice={setNotice}
      />
    )
  }

  if (route === ROUTES.ADMIN_REPORTS) {
    page = (
      <ReportPage
        setNotice={setNotice}
      />
    )
  }

  return (
    <AdminRoute>
      <AdminLayout
        route={route}
        setRoute={setRoute}
      >
        {page}
      </AdminLayout>
    </AdminRoute>
  )
}
