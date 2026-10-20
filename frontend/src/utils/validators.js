export function isRequired(value) {
  return value !== undefined && value !== null && String(value).trim() !== ''
}

export function validatePhone(value, { required = false } = {}) {
  const phone = String(value ?? '').trim()
  if (!phone) {
    return required ? 'Vui lòng nhập số điện thoại.' : ''
  }
  if (!/^\d+$/.test(phone)) {
    return 'Số điện thoại không được chứa chữ cái hoặc ký tự đặc biệt.'
  }
  if (!/^\d{9,11}$/.test(phone)) {
    return 'Số điện thoại phải gồm từ 9 đến 11 chữ số.'
  }
  return ''
}

export function isPositiveNumber(value) {
  return Number(value) > 0
}

export function isEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}
