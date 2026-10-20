import { useState } from 'react'
import { AuthLayout } from '../../layouts/AuthLayout'
import * as authService from '../../services/authService'
import { ROUTES } from '../../utils/constants'
import { isEmail } from '../../utils/validators'

export function ForgotPasswordPage({ setRoute }) {
  const [email, setEmail] = useState('')
  const [feedback, setFeedback] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function submit(event) {
    event.preventDefault()
    if (!isEmail(email)) {
      setFeedback({ type: 'error', message: 'Vui lòng nhập địa chỉ email hợp lệ.' })
      return
    }
    setIsSubmitting(true)
    setFeedback(null)
    try {
      const normalizedEmail = email.trim().toLowerCase()
      await authService.requestPasswordReset(normalizedEmail)
      sessionStorage.setItem('fashion:password-reset-email', normalizedEmail)
      setRoute(ROUTES.RESET_PASSWORD)
    } catch (error) {
      setFeedback({ type: 'error', message: `Không thể gửi yêu cầu: ${error.message}` })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout title="Quên mật khẩu" active={ROUTES.LOGIN} setRoute={setRoute}>
      <p>Nhập email đăng ký. Nếu tài khoản đang hoạt động, mã xác thực sẽ được gửi đến hộp thư của bạn.</p>
      {feedback && <p className={`auth-feedback ${feedback.type}`} role="alert">{feedback.message}</p>}
      <form className="form-grid auth-form" onSubmit={submit}>
        <label>Email
          <input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        </label>
        <div className="auth-actions"><button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Đang gửi mã…' : 'Gửi mã xác thực'}</button></div>
        <button type="button" className="text-button" onClick={() => setRoute(ROUTES.LOGIN)}>Quay lại đăng nhập</button>
      </form>
    </AuthLayout>
  )
}
