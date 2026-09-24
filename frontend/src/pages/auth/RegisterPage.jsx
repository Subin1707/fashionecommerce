import { useState } from 'react'
import { AuthLayout } from '../../layouts/AuthLayout'
import * as authService from '../../services/authService'
import { ROUTES } from '../../utils/constants'
import { isEmail, isRequired } from '../../utils/validators'

const initialForm = {
  fullName: '',
  email: '',
  password: '',
  phone: '',
  address: '',
}

export function RegisterPage({ setNotice, setRoute }) {
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '', address: '' })
  const [errors, setErrors] = useState({})
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const nextErrors = validateRegisterForm(form)
    setErrors(nextErrors)

    if (Object.keys(nextErrors).length) {
      setNotice('Vui lòng kiểm tra lại thông tin đăng ký.')
      return
    }

    try {
      setIsSubmitting(true)
      await authService.register(form)
      setForm(initialForm)
      setNotice('Đăng ký thành công, bạn có thể đăng nhập.')
      setRoute(ROUTES.LOGIN)
    } catch (error) {
      setNotice(`Đăng ký thất bại: ${error.message}`)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout title={<>Tạo tài khoản<br /><em>của bạn</em></>} active={ROUTES.REGISTER} setRoute={setRoute}>
      <form className="form-grid auth-form register-form" onSubmit={submit}>
        <label>Họ và tên
          <input value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
          {errors.fullName && <small className="field-error">{errors.fullName}</small>}
        </label>
        <label>Email
          <input value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
          {errors.email && <small className="field-error">{errors.email}</small>}
        </label>
        <label>Mật khẩu
          <span className="password-field"><input type={showPassword ? 'text' : 'password'} autoComplete="new-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /><button type="button" className="password-toggle" onClick={() => setShowPassword(!showPassword)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></span>
          {errors.password && <small className="field-error">{errors.password}</small>}
        </label>
        <label>Số điện thoại
          <input value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
          {errors.phone && <small className="field-error">{errors.phone}</small>}
        </label>
        <label>Địa chỉ
          <textarea value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} />
          {errors.address && <small className="field-error">{errors.address}</small>}
        </label>
        <div className="auth-actions">
          <button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Đang tạo tài khoản...' : 'Đăng ký'}</button>
        </div>
      </form>
    </AuthLayout>
  )
}

function validateRegisterForm(form) {
  const errors = {}
  if (!isRequired(form.fullName)) errors.fullName = 'Vui lòng nhập họ và tên.'
  if (!isEmail(form.email)) errors.email = 'Email không hợp lệ.'
  if (!isRequired(form.password) || form.password.length < 6) errors.password = 'Mật khẩu cần ít nhất 6 ký tự.'
  if (!isRequired(form.phone)) errors.phone = 'Vui lòng nhập số điện thoại.'
  if (!isRequired(form.address)) errors.address = 'Vui lòng nhập địa chỉ giao hàng.'
  return errors
}
