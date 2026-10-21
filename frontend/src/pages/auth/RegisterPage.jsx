import { useState } from 'react'
import { AuthLayout } from '../../layouts/AuthLayout'
import * as authService from '../../services/authService'
import { ROUTES } from '../../utils/constants'
import { isEmail, isRequired, validatePhone } from '../../utils/validators'

const initialForm = { fullName: '', email: '', password: '', phone: '', address: '' }

export function RegisterPage({ setRoute }) {
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [feedback, setFeedback] = useState(null)
  const [otpStep, setOtpStep] = useState(false)
  const [code, setCode] = useState('')

  async function submit(event) {
    event.preventDefault()
    const nextErrors = validateRegisterForm(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) {
      setFeedback({ type: 'error', message: 'Vui lòng kiểm tra lại thông tin đăng ký.' })
      return
    }

    setIsSubmitting(true)
    setFeedback(null)
    try {
      await authService.register(form)
      setOtpStep(true)
      setFeedback({ type: 'success', message: `Mã xác thực đã được gửi đến ${form.email}.` })
    } catch (error) {
      setFeedback({ type: 'error', message: `Không thể gửi mã xác thực: ${error.message}` })
    } finally {
      setIsSubmitting(false)
    }
  }

  async function verify(event) {
    event.preventDefault()
    if (!/^\d{6}$/.test(code)) {
      setFeedback({ type: 'error', message: 'Vui lòng nhập mã OTP gồm 6 chữ số.' })
      return
    }
    setIsSubmitting(true)
    setFeedback(null)
    try {
      await authService.verifyOtp({ email: form.email, code })
      sessionStorage.setItem('fashion:auth-feedback', JSON.stringify({
        type: 'success',
        message: 'Xác thực email thành công. Hãy đăng nhập bằng tài khoản vừa tạo.',
      }))
      setRoute(ROUTES.LOGIN)
    } catch (error) {
      setFeedback({ type: 'error', message: `Xác thực OTP thất bại: ${error.message}` })
    } finally {
      setIsSubmitting(false)
    }
  }

  async function resendOtp() {
    setIsSubmitting(true)
    setFeedback(null)
    try {
      await authService.register(form)
      setFeedback({ type: 'success', message: `Mã OTP mới đã được gửi đến ${form.email}.` })
    } catch (error) {
      setFeedback({ type: 'error', message: `Chưa thể gửi lại OTP: ${error.message}` })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout title={<>Tạo tài khoản<br /><em>của bạn</em></>} active={ROUTES.REGISTER} setRoute={setRoute}>
      {feedback && <p className={`auth-feedback ${feedback.type}`} role="status" aria-live="polite">{feedback.message}</p>}
      {otpStep ? (
        <form className="form-grid auth-form register-form" onSubmit={verify}>
          <p>Nhập mã OTP gồm 6 chữ số đã gửi đến <strong>{form.email}</strong>. Mã có hiệu lực trong 1 phút. Nhập sai 5 lần sẽ tạm khóa đăng ký trong 5 phút.</p>
          <label>Mã xác thực OTP
            <input inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))} />
          </label>
          <div className="auth-actions"><button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Đang xác thực...' : 'Xác thực email'}</button></div>
          <button type="button" className="text-button" disabled={isSubmitting} onClick={resendOtp}>Gửi lại mã OTP</button>
        </form>
      ) : (
        <form className="form-grid auth-form register-form" onSubmit={submit}>
          <label>Họ và tên
            <input autoComplete="name" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
            {errors.fullName && <small className="field-error">{errors.fullName}</small>}
          </label>
          <label>Email
            <input autoComplete="email" type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
            {errors.email && <small className="field-error">{errors.email}</small>}
          </label>
          <label>Mật khẩu
            <span className="password-field"><input type={showPassword ? 'text' : 'password'} autoComplete="new-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /><button type="button" className="password-toggle" onClick={() => setShowPassword(!showPassword)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></span>
            {errors.password && <small className="field-error">{errors.password}</small>}
          </label>
          <label>Số điện thoại
            <input type="tel" inputMode="numeric" autoComplete="tel" maxLength={20} value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
            {errors.phone && <small className="field-error">{errors.phone}</small>}
          </label>
          <label>Địa chỉ
            <textarea autoComplete="street-address" value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} />
            {errors.address && <small className="field-error">{errors.address}</small>}
          </label>
          <div className="auth-actions"><button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Đang gửi OTP...' : 'Đăng ký'}</button></div>
        </form>
      )}
    </AuthLayout>
  )
}

function validateRegisterForm(form) {
  const errors = {}
  if (!isRequired(form.fullName)) errors.fullName = 'Vui lòng nhập họ và tên.'
  if (!isEmail(form.email)) errors.email = 'Email không hợp lệ.'
  if (!isRequired(form.password) || form.password.length < 6) errors.password = 'Mật khẩu cần ít nhất 6 ký tự.'
  const phoneError = validatePhone(form.phone, { required: true })
  if (phoneError) errors.phone = phoneError
  if (!isRequired(form.address)) errors.address = 'Vui lòng nhập địa chỉ giao hàng.'
  return errors
}
