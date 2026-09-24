import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../hooks/useAuth'
import * as userService from '../../services/userService'

const emptyProfile = {
  fullName: '',
  phone: '',
  address: '',
  avatar: '',
}

export function ProfilePage({ setNotice }) {
  const { auth } = useAuth()
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(emptyProfile)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false

    userService.getProfile()
      .then((data) => {
        if (cancelled) return
        setProfile(data)
        setForm(toProfileForm(data))
      })
      .catch((error) => {
        if (cancelled) return
        setNotice?.(`Không đọc được hồ sơ: ${error.message}`)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [setNotice])

  const initials = useMemo(() => {
    const name = form.fullName || profile?.fullName || auth?.email || 'F'
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase()
  }, [auth?.email, form.fullName, profile?.fullName])

  const completion = profileCompletion(form)
  const joinedDate = formatDate(profile?.createdAt)
  const updatedDate = formatDate(profile?.updatedAt)

  async function submit(event) {
    event.preventDefault()
    setSaving(true)
    try {
      const updated = await userService.updateProfile(form)
      setProfile(updated)
      setForm(toProfileForm(updated))
      setNotice?.('Đã cập nhật hồ sơ.')
    } catch (error) {
      setNotice?.(`Không cập nhật được hồ sơ: ${error.message}`)
    } finally {
      setSaving(false)
    }
  }

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  if (loading) {
    return (
      <section className="profile-page">
        <div className="profile-loading-card" />
        <div className="profile-loading-card" />
      </section>
    )
  }

  return (
    <section className="profile-page">
      <div className="profile-hero-panel">
        <div className="profile-avatar-ring">
          {form.avatar ? <img src={form.avatar} alt={form.fullName || auth?.email} /> : <span>{initials}</span>}
        </div>

        <div className="profile-identity">
          <p className="eyebrow">Tài khoản cá nhân</p>
          <h1>{form.fullName || 'Cập nhật họ tên'}</h1>
          <p>{profile?.email || auth?.email || '-'}</p>
        </div>

        <div className="profile-completion">
          <div>
            <span>Hoàn thiện hồ sơ</span>
            <strong>{completion}%</strong>
          </div>
          <div className="profile-progress-track">
            <span style={{ width: `${completion}%` }} />
          </div>
        </div>

        <div className="profile-stat-grid">
          <div><span>Vai trò</span><strong>{profile?.role || auth?.role || '-'}</strong></div>
          <div><span>Trạng thái</span><strong>{profile?.status || 'ACTIVE'}</strong></div>
          <div><span>Ngày tạo</span><strong>{joinedDate}</strong></div>
          <div><span>Cập nhật</span><strong>{updatedDate}</strong></div>
        </div>
      </div>

      <form className="profile-edit-panel" onSubmit={submit}>
        <div className="profile-panel-heading">
          <div>
            <p className="eyebrow">Thông tin giao hàng</p>
            <h2>Hồ sơ mua sắm</h2>
          </div>
          <span>{profile?.email || auth?.email}</span>
        </div>

        <div className="profile-form-grid">
          <label>Họ và tên
            <input value={form.fullName} onChange={(event) => updateField('fullName', event.target.value)} />
          </label>

          <label>Số điện thoại
            <input value={form.phone} onChange={(event) => updateField('phone', event.target.value)} />
          </label>

          <label className="profile-wide-field">Địa chỉ mặc định
            <textarea value={form.address} onChange={(event) => updateField('address', event.target.value)} />
          </label>

          <label className="profile-wide-field">Ảnh đại diện URL
            <input value={form.avatar} onChange={(event) => updateField('avatar', event.target.value)} placeholder="https://..." />
          </label>
        </div>

        <div className="profile-actions">
          <button type="button" className="ghost" onClick={() => setForm(toProfileForm(profile))} disabled={saving}>
            Hoàn tác
          </button>
          <button type="submit" disabled={saving}>
            {saving ? 'Đang lưu...' : 'Lưu hồ sơ'}
          </button>
        </div>
      </form>
    </section>
  )
}

function toProfileForm(profile) {
  return {
    fullName: profile?.fullName || '',
    phone: profile?.phone || '',
    address: profile?.address || '',
    avatar: profile?.avatar || '',
  }
}

function profileCompletion(profile) {
  const fields = ['fullName', 'phone', 'address', 'avatar']
  const filled = fields.filter((field) => profile[field]?.trim()).length
  return Math.round((filled / fields.length) * 100)
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value))
}
