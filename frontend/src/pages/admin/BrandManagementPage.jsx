import { useCallback, useEffect, useMemo, useState } from 'react'
import * as adminService from '../../services/adminService'
import * as brandService from '../../services/brandService'

const emptyBrand = {
  name: '',
  slug: '',
  description: '',
  logoUrl: '',
  isActive: true,
}

export function BrandManagementPage({ setNotice }) {
  const [brands, setBrands] = useState([])
  const [products, setProducts] = useState([])
  const [editingBrand, setEditingBrand] = useState(null)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async function loadData() {
    setLoading(true)
    try {
      const [brandRows, productRows] = await Promise.all([
        brandService.getBrands(),
        adminService.getAdminProducts({ size: 100 }),
      ])
      setBrands(brandRows)
      setProducts(productRows)
    } catch (error) {
      setNotice?.(`Chưa đọc được thương hiệu: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }, [setNotice])

  useEffect(() => {
    Promise.resolve().then(loadData)
  }, [loadData])

  const brandStats = useMemo(() => {
    return products.reduce((stats, product) => {
      if (!product.brandId) return stats
      const current = stats[product.brandId] || { total: 0, active: 0, stock: 0 }
      return {
        ...stats,
        [product.brandId]: {
          total: current.total + 1,
          active: current.active + (product.status === 'ACTIVE' ? 1 : 0),
          stock: current.stock + Number(product.totalStock || 0),
        },
      }
    }, {})
  }, [products])

  const filteredBrands = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return brands
      .filter((brand) => {
        const matchesQuery = !normalizedQuery
          || brand.name?.toLowerCase().includes(normalizedQuery)
          || brand.slug?.toLowerCase().includes(normalizedQuery)
          || brand.description?.toLowerCase().includes(normalizedQuery)
          || String(brand.id).includes(normalizedQuery)
        const matchesStatus = statusFilter === 'ALL'
          || (statusFilter === 'ACTIVE' && brand.isActive)
          || (statusFilter === 'INACTIVE' && !brand.isActive)

        return matchesQuery && matchesStatus
      })
      .sort((a, b) => Number(Boolean(b.isActive)) - Number(Boolean(a.isActive)) || a.name.localeCompare(b.name))
  }, [brands, query, statusFilter])

  const summary = useMemo(() => ({
    total: brands.length,
    active: brands.filter((brand) => brand.isActive).length,
    inactive: brands.filter((brand) => !brand.isActive).length,
    withProducts: brands.filter((brand) => Number(brandStats[brand.id]?.total || 0) > 0).length,
  }), [brands, brandStats])

  function startCreate() {
    setEditingBrand({ ...emptyBrand })
  }

  function startEdit(brand) {
    setEditingBrand({
      name: brand.name || '',
      slug: brand.slug || '',
      description: brand.description || '',
      logoUrl: brand.logoUrl || '',
      isActive: Boolean(brand.isActive),
      id: brand.id,
    })
  }

  function updateDraft(patch) {
    setEditingBrand((current) => ({ ...current, ...patch }))
  }

  async function submit(event) {
    event.preventDefault()
    if (!editingBrand) return

    setSaving(true)
    const payload = toBrandPayload(editingBrand)

    try {
      const saved = editingBrand.id
        ? await brandService.updateBrand(editingBrand.id, payload)
        : await brandService.createBrand(payload)

      setBrands((rows) => editingBrand.id
        ? rows.map((brand) => (brand.id === saved.id ? saved : brand))
        : [saved, ...rows])
      setEditingBrand(null)
      setNotice?.(editingBrand.id ? `Đã cập nhật thương hiệu #${saved.id}.` : `Đã tạo thương hiệu #${saved.id}.`)
    } catch (error) {
      setNotice?.(`Không lưu được thương hiệu: ${error.message}`)
    } finally {
      setSaving(false)
    }
  }

  async function removeBrand(brand) {
    if (!window.confirm(`Xóa thương hiệu "${brand.name}"?`)) return

    try {
      await brandService.deleteBrand(brand.id)
      setBrands((rows) => rows.filter((row) => row.id !== brand.id))
      if (editingBrand?.id === brand.id) setEditingBrand(null)
      setNotice?.(`Đã xóa thương hiệu #${brand.id}.`)
    } catch (error) {
      setNotice?.(`Không xóa được thương hiệu: ${error.message}`)
    }
  }

  return (
    <section className="brand-admin-page">
      <div className="brand-admin-hero">
        <div>
          <p className="eyebrow">Thương hiệu</p>
          <h1>Quản lý thương hiệu</h1>
          <p>Quản trị logo, slug, mô tả và trạng thái hiển thị của từng thương hiệu trong catalog.</p>
        </div>
        <button type="button" onClick={startCreate}>+ Thêm thương hiệu</button>
      </div>

      <div className="brand-admin-kpi-grid">
        <BrandKpi label="Tổng thương hiệu" value={summary.total} />
        <BrandKpi label="Đang hiển thị" value={summary.active} tone="success" />
        <BrandKpi label="Tạm ẩn" value={summary.inactive} tone="warning" />
        <BrandKpi label="Có sản phẩm" value={summary.withProducts} />
      </div>

      <div className="brand-admin-toolbar">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Tìm theo tên, slug, mô tả hoặc ID..."
        />
        <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          <option value="ALL">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang hiển thị</option>
          <option value="INACTIVE">Tạm ẩn</option>
        </select>
        <button type="button" className="ghost" onClick={loadData} disabled={loading}>
          {loading ? 'Đang tải...' : 'Làm mới'}
        </button>
      </div>

      <div className="brand-admin-layout">
        <div className="brand-admin-list-panel">
          <div className="brand-admin-panel-heading">
            <div>
              <p className="eyebrow">Danh sách</p>
              <h2>{filteredBrands.length} thương hiệu</h2>
            </div>
            <span>{products.length} sản phẩm trong catalog</span>
          </div>

          {loading && <div className="admin-product-loading" />}
          {!loading && filteredBrands.length === 0 && <p className="empty-state">Chưa có thương hiệu phù hợp.</p>}

          <div className="brand-admin-grid">
            {!loading && filteredBrands.map((brand) => (
              <BrandCard
                key={brand.id}
                brand={brand}
                stats={brandStats[brand.id]}
                selected={editingBrand?.id === brand.id}
                onEdit={startEdit}
                onRemove={removeBrand}
              />
            ))}
          </div>
        </div>

        <aside className="brand-admin-editor-panel">
          {editingBrand ? (
            <BrandForm
              value={editingBrand}
              saving={saving}
              onChange={updateDraft}
              onSubmit={submit}
              onCancel={() => setEditingBrand(null)}
            />
          ) : (
            <div className="brand-admin-empty-editor">
              <p className="eyebrow">Biểu mẫu</p>
              <h2>Chọn thương hiệu để chỉnh sửa</h2>
              <p>Hoặc tạo thương hiệu mới để gắn sản phẩm vào catalog.</p>
              <button type="button" onClick={startCreate}>Thêm thương hiệu</button>
            </div>
          )}
        </aside>
      </div>
    </section>
  )
}

function BrandKpi({ label, value, tone = '' }) {
  return (
    <article className={`brand-admin-kpi ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function BrandCard({ brand, stats, selected, onEdit, onRemove }) {
  return (
    <article className={`brand-admin-card ${selected ? 'selected' : ''}`}>
      <div className="brand-admin-logo">
        {brand.logoUrl ? <img src={brand.logoUrl} alt={brand.name} /> : <span>{brand.name?.slice(0, 2) || 'BR'}</span>}
      </div>

      <div className="brand-admin-card-body">
        <div className="brand-admin-card-head">
          <div>
            <p className="eyebrow">#{brand.id} · {brand.slug}</p>
            <h2>{brand.name}</h2>
          </div>
          <span className={`brand-admin-status ${brand.isActive ? 'active' : 'inactive'}`}>
            {brand.isActive ? 'Đang hiển thị' : 'Tạm ẩn'}
          </span>
        </div>

        <p>{brand.description || 'Chưa có mô tả thương hiệu.'}</p>

        <div className="brand-admin-card-meta">
          <span>{Number(stats?.total || 0)} sản phẩm</span>
          <span>{Number(stats?.active || 0)} đang bán</span>
          <span>{Number(stats?.stock || 0)} tồn kho</span>
        </div>

        <div className="brand-admin-actions">
          <button type="button" onClick={() => onEdit(brand)}>Sửa</button>
          <button type="button" className="danger" onClick={() => onRemove(brand)}>Xóa</button>
        </div>
      </div>
    </article>
  )
}

function BrandForm({ value, saving, onChange, onSubmit, onCancel }) {
  function updateName(name) {
    onChange({
      name,
      slug: value.slug ? value.slug : slugify(name),
    })
  }

  return (
    <form className="brand-admin-form" onSubmit={onSubmit}>
      <div className="brand-admin-panel-heading">
        <div>
          <p className="eyebrow">Biểu mẫu</p>
          <h2>{value.id ? `Sửa thương hiệu #${value.id}` : 'Thêm thương hiệu'}</h2>
        </div>
        <span>{value.isActive ? 'Đang hiển thị' : 'Tạm ẩn'}</span>
      </div>

      <div className="brand-admin-preview">
        <div className="brand-admin-logo large">
          {value.logoUrl ? <img src={value.logoUrl} alt={value.name || 'Logo'} /> : <span>{value.name?.slice(0, 2) || 'BR'}</span>}
        </div>
        <div>
          <strong>{value.name || 'Tên thương hiệu'}</strong>
          <small>{value.slug || 'slug-thuong-hieu'}</small>
        </div>
      </div>

      <label>Tên thương hiệu
        <input value={value.name} onChange={(event) => updateName(event.target.value)} required />
      </label>

      <label>Slug
        <input value={value.slug} onChange={(event) => onChange({ slug: slugify(event.target.value) })} required />
      </label>

      <label>Logo URL
        <input value={value.logoUrl} onChange={(event) => onChange({ logoUrl: event.target.value })} placeholder="https://..." />
      </label>

      <label>Mô tả
        <textarea value={value.description} onChange={(event) => onChange({ description: event.target.value })} />
      </label>

      <label className="brand-admin-toggle">
        <input type="checkbox" checked={value.isActive} onChange={(event) => onChange({ isActive: event.target.checked })} />
        <span>Hiển thị thương hiệu</span>
      </label>

      <div className="brand-admin-submit-row">
        <button type="button" className="ghost" onClick={onCancel} disabled={saving}>Hủy</button>
        <button type="submit" disabled={saving}>{saving ? 'Đang lưu...' : value.id ? 'Lưu thay đổi' : 'Tạo thương hiệu'}</button>
      </div>
    </form>
  )
}

function toBrandPayload(brand) {
  return {
    name: brand.name.trim(),
    slug: slugify(brand.slug || brand.name),
    description: brand.description?.trim() || null,
    logoUrl: brand.logoUrl?.trim() || null,
    isActive: Boolean(brand.isActive),
  }
}

function slugify(value) {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
    .replace(/đ/g, 'd')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}
