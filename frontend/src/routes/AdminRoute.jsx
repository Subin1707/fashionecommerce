import { useAuth } from '../hooks/useAuth'

export function AdminRoute({ children }) {
  const { isAdmin } = useAuth()
  if (!isAdmin) return <p className="empty-state">Đăng nhập bằng tài khoản ADMIN để truy cập màn hình quản trị.</p>
  return children
}
