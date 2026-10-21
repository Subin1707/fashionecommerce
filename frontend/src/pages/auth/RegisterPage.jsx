import { useEffect, useState } from 'react'
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
  const [lockedUntil, setLockedUntil] = useState(0)
  const [otpExpiresAt, setOtpExpiresAt] = useState(0)
  const [resendAvailableAt, setResendAvailableAt] = useState(0)
  const [clock, setClock] = useState(Date.now())
  const locked = lockedUntil > clock
  const secondsLeft = Math.max(0, Math.ceil((lockedUntil - clock) / 1000))
  const otpSecondsLeft = Math.max(0, Math.ceil((otpExpiresAt - clock) / 1000))
  const otpExpired = otpExpiresAt > 0 && otpSecondsLeft === 0

  useEffect(() => {
    if (!lockedUntil && !otpStep) return undefined
    const timer = window.setInterval(() => {
      const now = Date.now()
      setClock(now)
      if (now >= lockedUntil) setLockedUntil(0)
    }, 250)
    return () => window.clearInterval(timer)
  }, [lockedUntil, otpStep])

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
      const expiresAt = Date.now() + 60_000
      setOtpExpiresAt(expiresAt)
      setResendAvailableAt(expiresAt)
      setClock(Date.now())
      setFeedback({ type: 'success', message: `Mã xác thực đã được gửi đến ${form.email}.` })
    } catch (error) {
      const message = String(error.message || '')
      if (/OTP/i.test(message) && /5/.test(message) && /ph/i.test(message)) {
        const minutes = Number(message.match(/sau\s+(\d+)\s+ph/i)?.[1]) || 5
        const retryAt = Date.now() + minutes * 60_000
        setOtpStep(true)
        setCode('')
        setOtpExpiresAt(0)
        setResendAvailableAt(retryAt)
        setLockedUntil(retryAt)
        setClock(Date.now())
        setFeedback({ type: 'error', message: 'Xác minh OTP đang bị khóa do nhập sai quá 5 lần.' })
        return
      }
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
      const result = await authService.verifyOtp({ email: form.email, code })
      if (!result.verified) {
        setFeedback({ type: 'error', message: result.message || 'Mã OTP không đúng.' })
        if (result.locked) setLockedUntil(Date.now() + Number(result.retryAfterSeconds || 300) * 1000)
        return
      }
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
      const expiresAt = Date.now() + 60_000
      setOtpExpiresAt(expiresAt)
      setResendAvailableAt(expiresAt)
      setClock(Date.now())
      setCode('')
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
          <p>Nhập mã OTP gồm 6 chữ số đã gửi đến <strong>{form.email}</strong>. Mã có hiệu lực trong 1 phút. Nhập sai 5 lần sẽ khóa xác minh trong 5 phút.</p>
          <p className={otpExpired ? 'field-error' : 'otp-expiry-countdown'} role="timer" aria-live="polite">
            {locked ? 'Xác minh OTP đang tạm khóa.' : otpExpired ? 'Mã OTP đã hết hạn. Hãy gửi lại mã mới.' : `Mã OTP còn hiệu lực ${Math.floor(otpSecondsLeft / 60)}:${String(otpSecondsLeft % 60).padStart(2, '0')}.`}
          </p>
          <label>Mã xác thực OTP
            <input inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} disabled={locked || otpExpired} onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))} />
          </label>
          {locked && <p className="field-error" role="status">Đã khóa nhập OTP. Có thể thử lại sau {Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, '0')}.</p>}
          <div className="auth-actions"><button type="submit" disabled={isSubmitting || locked || otpExpired}>{isSubmitting ? 'Đang xác thực...' : locked ? 'Đang khóa xác minh' : otpExpired ? 'Mã OTP đã hết hạn' : 'Xác thực email'}</button></div>
          <button type="button" className="text-button" disabled={isSubmitting || locked || clock < resendAvailableAt} onClick={resendOtp}>{clock < resendAvailableAt ? `Gửi lại mã sau ${Math.ceil((resendAvailableAt - clock) / 1000)} giây` : 'Gửi lại mã OTP'}</button>
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
