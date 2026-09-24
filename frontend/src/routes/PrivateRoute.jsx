import { useAuth } from '../hooks/useAuth'

export function PrivateRoute({ children, fallback = 'Đăng nhập bằng tài khoản CUSTOMER để sử dụng tính năng này.' }) {
  const { isCustomer } = useAuth()
  if (!isCustomer) return <p className="empty-state">{fallback}</p>
  return children
}
