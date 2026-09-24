import { apiRequest } from './api'

const STORAGE_KEY = 'fashion:vouchers_cache'

const defaultVouchers = [
  {
    id: 1,
    code: 'FASHION10',
    discountType: 'PERCENTAGE',
    discountValue: 10,
    minOrderValue: 200000,
    maxDiscount: 50000,
    startDate: '2026-01-01T00:00:00',
    endDate: '2026-12-31T23:59:59',
    status: 'ACTIVE',
  },
  {
    id: 2,
    code: 'VIP50K',
    discountType: 'FIXED_AMOUNT',
    discountValue: 50000,
    minOrderValue: 500000,
    maxDiscount: 50000,
    startDate: '2026-01-01T00:00:00',
    endDate: '2026-12-31T23:59:59',
    status: 'ACTIVE',
  },
  {
    id: 3,
    code: 'FREESHIP',
    discountType: 'FIXED_AMOUNT',
    discountValue: 30000,
    minOrderValue: 300000,
    maxDiscount: 30000,
    startDate: '2026-01-01T00:00:00',
    endDate: '2026-12-31T23:59:59',
    status: 'ACTIVE',
  },
  {
    id: 4,
    code: 'WELCOME20',
    discountType: 'PERCENTAGE',
    discountValue: 20,
    minOrderValue: 400000,
    maxDiscount: 100000,
    startDate: '2026-06-01T00:00:00',
    endDate: '2026-12-31T23:59:59',
    status: 'ACTIVE',
  },
  {
    id: 5,
    code: 'SUMMERSALE',
    discountType: 'PERCENTAGE',
    discountValue: 15,
    minOrderValue: 350000,
    maxDiscount: 80000,
    startDate: '2026-06-01T00:00:00',
    endDate: '2026-08-31T23:59:59',
    status: 'EXPIRED',
  },
]

function getCachedVouchers() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(defaultVouchers))
      return defaultVouchers
    }
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : defaultVouchers
  } catch {
    return defaultVouchers
  }
}

function saveCachedVouchers(list) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
  } catch {
    // ignore
  }
}

export async function getVouchers() {
  try {
    const data = await apiRequest('/api/vouchers')
    if (Array.isArray(data) && data.length > 0) {
      saveCachedVouchers(data)
      return data
    }
  } catch {
    // fallback to cache
  }
  return getCachedVouchers()
}

export async function getVoucherById(id) {
  try {
    return await apiRequest(`/api/vouchers/${id}`)
  } catch {
    const list = getCachedVouchers()
    return list.find((v) => String(v.id) === String(id)) || null
  }
}

export async function createVoucher(payload) {
  let created = null
  try {
    created = await apiRequest('/api/vouchers', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  } catch (error) {
    if (error.message?.includes('tồn tại') || error.message?.includes('hợp lệ')) {
      throw error
    }
  }

  const normalized = created || {
    ...payload,
    id: Date.now(),
    code: String(payload.code).trim().toUpperCase(),
    discountType: payload.discountType,
    discountValue: Number(payload.discountValue),
    minOrderValue: Number(payload.minOrderValue || 0),
    maxDiscount: Number(payload.maxDiscount || 0),
    startDate: payload.startDate || new Date().toISOString(),
    endDate: payload.endDate || new Date(Date.now() + 30 * 86400000).toISOString(),
    status: payload.status || 'ACTIVE',
  }

  const current = getCachedVouchers()
  saveCachedVouchers([normalized, ...current.filter((v) => v.code !== normalized.code)])
  return normalized
}

export async function updateVoucher(id, payload) {
  let updated = null
  try {
    updated = await apiRequest(`/api/vouchers/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  } catch (error) {
    if (error.message?.includes('tồn tại') || error.message?.includes('hợp lệ')) {
      throw error
    }
  }

  const normalized = updated || {
    ...payload,
    id,
    code: String(payload.code).trim().toUpperCase(),
  }

  const current = getCachedVouchers()
  const next = current.map((v) => (String(v.id) === String(id) ? { ...v, ...normalized } : v))
  saveCachedVouchers(next)
  return normalized
}

export async function deleteVoucher(id) {
  try {
    await apiRequest(`/api/vouchers/${id}`, { method: 'DELETE' })
  } catch {
    // ignore
  }
  const current = getCachedVouchers()
  saveCachedVouchers(current.filter((v) => String(v.id) !== String(id)))
}

export function validateVoucher(code, orderValue) {
  const params = new URLSearchParams({ code, orderValue: String(orderValue) })
  return apiRequest(`/api/vouchers/validate?${params}`)
}
