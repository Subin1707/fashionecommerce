import { useState } from 'react'
import { AuthLayout } from '../../layouts/AuthLayout'
import * as authService from '../../services/authService'
import { ROUTES } from '../../utils/constants'
import { isEmail } from '../../utils/validators'

export function ResetPasswordPage({ setRoute }) {
  const [email, setEmail] = useState(() => sessionStorage.getItem('fashion:password-reset-email') || '')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [feedback, setFeedback] = useState({
    type: 'success',
    message: 'Nếu email thuộc tài khoản đang hoạt động, mã xác thực sẽ được gửi đến hộp thư đó.',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function submit(event) {
    event.preventDefault()
    if (!isEmail(email)) return setFeedback({ type: 'error', message: 'Vui lòng nhập địa chỉ email hợp lệ.' })
    if (!/^\d{6}$/.test(code)) return setFeedback({ type: 'error', message: 'Mã xác thực phải gồm 6 chữ số.' })
    if (password.length < 8 || password.length > 100) return setFeedback({ type: 'error', message: 'Mật khẩu mới phải có từ 8 đến 100 ký tự.' })
    if (password !== confirmation) return setFeedback({ type: 'error', message: 'Mật khẩu nhập lại chưa khớp.' })

    setIsSubmitting(true)
    setFeedback(null)
    try {
      await authService.resetPassword({ email: email.trim().toLowerCase(), code, newPassword: password })
      sessionStorage.removeItem('fashion:password-reset-email')
      sessionStorage.setItem('fashion:auth-feedback', JSON.stringify({ type: 'success', message: 'Đổi mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.' }))
      setRoute(ROUTES.LOGIN)
    } catch (error) {
      setFeedback({ type: 'error', message: `Không thể đổi mật khẩu: ${error.message}` })
    } finally {
      setIsSubmitting(false)
    }
  }

  async function resendCode() {
    if (!isEmail(email)) return setFeedback({ type: 'error', message: 'Vui lòng nhập địa chỉ email hợp lệ.' })
    setIsSubmitting(true)
    try {
      await authService.requestPasswordReset(email.trim().toLowerCase())
      setFeedback({ type: 'success', message: 'Nếu email thuộc tài khoản đang hoạt động, mã mới sẽ được gửi đến hộp thư đó.' })
    } catch (error) {
      setFeedback({ type: 'error', message: `Không thể gửi lại mã: ${error.message}` })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout title="Đặt lại mật khẩu" active={ROUTES.LOGIN} setRoute={setRoute}>
      <p>Nhập mã 6 chữ số trong email và tạo mật khẩu mới. Mã có hiệu lực trong 10 phút.</p>
      {feedback && <p className={`auth-feedback ${feedback.type}`} role={feedback.type === 'error' ? 'alert' : 'status'}>{feedback.message}</p>}
      <form className="form-grid auth-form" onSubmit={submit}>
        <label>Email tài khoản
          <input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        </label>
        <label>Mã xác thực
          <input inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))} required />
        </label>
        <label>Mật khẩu mới
          <input type="password" autoComplete="new-password" minLength={8} maxLength={100} value={password} onChange={(event) => setPassword(event.target.value)} required />
        </label>
        <label>Nhập lại mật khẩu mới
          <input type="password" autoComplete="new-password" minLength={8} maxLength={100} value={confirmation} onChange={(event) => setConfirmation(event.target.value)} required />
        </label>
        <div className="auth-actions"><button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Đang cập nhật…' : 'Đổi mật khẩu'}</button></div>
        <button type="button" className="text-button" disabled={isSubmitting} onClick={resendCode}>Gửi lại mã xác thực</button>
        <button type="button" className="text-button" onClick={() => setRoute(ROUTES.LOGIN)}>Quay lại đăng nhập</button>
      </form>
    </AuthLayout>
  )
}
