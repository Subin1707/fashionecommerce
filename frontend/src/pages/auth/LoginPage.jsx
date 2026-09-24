import { useState } from 'react'
import { AuthLayout } from '../../layouts/AuthLayout'
import { useAuth } from '../../hooks/useAuth'
import { ROUTES } from '../../utils/constants'

export function LoginPage({ setNotice, setRoute }) {
  const { login } = useAuth()
  const [form, setForm] = useState({ email: '', password: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setIsSubmitting(true)
    try {
      const auth = await login(form)
      setNotice(`Đăng nhập thành công: ${auth.email} (${auth.role}).`)
      const returnRoute = sessionStorage.getItem('fashion:wishlist-return-route')
      sessionStorage.removeItem('fashion:wishlist-return-route')
      setRoute(auth.role === 'ADMIN' ? ROUTES.ADMIN_DASHBOARD : returnRoute || ROUTES.HOME)
    } catch (error) {
      setNotice(`Đăng nhập thất bại: ${error.message}`)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout title={<>Đăng nhập<br /><em>tài khoản</em></>} active={ROUTES.LOGIN} setRoute={setRoute}>
      <form className="form-grid auth-form" onSubmit={submit}>
        <label>Email<input autoComplete="email" placeholder="tên@email.com" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        <label>Mật khẩu
          <span className="password-field"><input autoComplete="current-password" type={showPassword ? 'text' : 'password'} placeholder="••••••••" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /><button type="button" className="password-toggle" aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setShowPassword(!showPassword)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></span>
        </label>
        <div className="auth-options"><label className="checkbox-label"><input type="checkbox" /> Ghi nhớ đăng nhập</label></div>
        <div className="auth-actions"><button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}</button></div>
        <div className="auth-register-hint">Chưa có tài khoản? <button type="button" className="text-button" onClick={() => setRoute(ROUTES.REGISTER)}>Đăng ký ngay →</button>
        </div>
      </form>
    </AuthLayout>
  )
}
