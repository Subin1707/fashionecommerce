import { useCallback, useEffect, useMemo, useState } from 'react'
import * as adminService from '../../services/adminService'
import * as categoryService from '../../services/categoryService'

const emptyCategory = {
  name: '',
  slug: '',
  description: '',
  parentId: '',
  isActive: true,
}

export function CategoryManagementPage({ setNotice }) {
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState([])
  const [editingCategory, setEditingCategory] = useState(null)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [hierarchyFilter, setHierarchyFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async function loadData() {
    setLoading(true)
    try {
      const [categoryRows, productRows] = await Promise.all([
        categoryService.getCategories(),
        adminService.getAdminProducts({ size: 100 }).catch(() => []),
      ])
      setCategories(Array.isArray(categoryRows) ? categoryRows : [])
      setProducts(Array.isArray(productRows) ? productRows : [])
    } catch (error) {
      setNotice?.(`Chưa đọc được danh mục: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }, [setNotice])

  useEffect(() => {
    Promise.resolve().then(loadData)
  }, [loadData])

  // Thống kê số sản phẩm và tồn kho theo từng category
  const categoryStats = useMemo(() => {
    return products.reduce((stats, product) => {
      if (!product.categoryId) return stats
      const current = stats[product.categoryId] || { total: 0, active: 0, stock: 0 }
      return {
        ...stats,
        [product.categoryId]: {
          total: current.total + 1,
          active: current.active + (product.status === 'ACTIVE' ? 1 : 0),
          stock: current.stock + Number(product.totalStock || 0),
        },
      }
    }, {})
  }, [products])

  // Đếm số danh mục con của từng category cha
  const childrenMap = useMemo(() => {
    return categories.reduce((acc, cat) => {
      const parentId = cat.parent?.id
      if (parentId) {
        acc[parentId] = (acc[parentId] || 0) + 1
      }
      return acc
    }, {})
  }, [categories])

  // Danh sách đã lọc và sắp xếp
  const filteredCategories = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return categories
      .filter((cat) => {
        const matchesQuery = !normalizedQuery
          || cat.name?.toLowerCase().includes(normalizedQuery)
          || cat.slug?.toLowerCase().includes(normalizedQuery)
          || cat.description?.toLowerCase().includes(normalizedQuery)
          || String(cat.id).includes(normalizedQuery)
          || cat.parent?.name?.toLowerCase().includes(normalizedQuery)

        const matchesStatus = statusFilter === 'ALL'
          || (statusFilter === 'ACTIVE' && cat.isActive)
          || (statusFilter === 'INACTIVE' && !cat.isActive)

        const isChild = Boolean(cat.parent?.id)
        const matchesHierarchy = hierarchyFilter === 'ALL'
          || (hierarchyFilter === 'ROOT' && !isChild)
          || (hierarchyFilter === 'CHILD' && isChild)

        return matchesQuery && matchesStatus && matchesHierarchy
      })
      .sort((a, b) => {
        // Ưu tiên danh mục đang hiển thị, sau đó theo tên
        return Number(Boolean(b.isActive)) - Number(Boolean(a.isActive)) || a.name.localeCompare(b.name)
      })
  }, [categories, query, statusFilter, hierarchyFilter])

  // KPIs tổng hợp
  const summary = useMemo(() => ({
    total: categories.length,
    active: categories.filter((c) => c.isActive).length,
    inactive: categories.filter((c) => !c.isActive).length,
    rootCount: categories.filter((c) => !c.parent?.id).length,
    withProducts: categories.filter((c) => Number(categoryStats[c.id]?.total || 0) > 0).length,
  }), [categories, categoryStats])

  function startCreate() {
    setEditingCategory({ ...emptyCategory })
  }

  function startCreateChild(parentCategory) {
    setEditingCategory({
      ...emptyCategory,
      parentId: String(parentCategory.id),
    })
  }

  function startEdit(cat) {
    setEditingCategory({
      id: cat.id,
      name: cat.name || '',
      slug: cat.slug || '',
      description: cat.description || '',
      parentId: cat.parent?.id ? String(cat.parent.id) : '',
      isActive: Boolean(cat.isActive),
    })
  }

  function updateDraft(patch) {
    setEditingCategory((current) => ({ ...current, ...patch }))
  }

  async function submit(event) {
    event.preventDefault()
    if (!editingCategory) return

    setSaving(true)
    const payload = toCategoryPayload(editingCategory)

    try {
      let saved
      if (editingCategory.id) {
        saved = await categoryService.updateCategory(editingCategory.id, payload)
        setCategories((rows) => rows.map((row) => (row.id === saved.id ? saved : row)))
        setNotice?.(`Đã cập nhật danh mục #${saved.id} ("${saved.name}").`)
      } else {
        saved = await categoryService.createCategory(payload)
        setCategories((rows) => [saved, ...rows])
        setNotice?.(`Đã tạo danh mục mới #${saved.id} ("${saved.name}").`)
      }

      setEditingCategory(null)
      // Tải lại để đồng bộ quan hệ cây cha - con
      loadData()
    } catch (error) {
      setNotice?.(`Không lưu được danh mục: ${error.message}`)
    } finally {
      setSaving(false)
    }
  }

  async function removeCategory(cat) {
    const childCount = childrenMap[cat.id] || 0
    const prodCount = categoryStats[cat.id]?.total || 0

    let confirmMsg = `Bạn có chắc muốn xóa danh mục "${cat.name}" (ID #${cat.id})?`
    if (childCount > 0) {
      confirmMsg += `\n⚠️ CẢNH BÁO: Danh mục này đang có ${childCount} danh mục con.`
    }
    if (prodCount > 0) {
      confirmMsg += `\n⚠️ CẢNH BÁO: Danh mục này đang gắn với ${prodCount} sản phẩm trong catalog.`
    }

    if (!window.confirm(confirmMsg)) return

    try {
      await categoryService.deleteCategory(cat.id)
      setCategories((rows) => rows.filter((row) => row.id !== cat.id))
      if (editingCategory?.id === cat.id) setEditingCategory(null)
      setNotice?.(`Đã xóa danh mục #${cat.id}.`)
      loadData()
    } catch (error) {
      setNotice?.(`Không xóa được danh mục: ${error.message}`)
    }
  }

  // Danh sách các danh mục có thể chọn làm cha (loại trừ chính nó và các con của nó)
  const availableParents = useMemo(() => {
    if (!editingCategory?.id) return categories
    return categories.filter((c) => c.id !== editingCategory.id && c.parent?.id !== editingCategory.id)
  }, [categories, editingCategory])

  return (
    <section className="category-admin-page">
      <div className="category-admin-hero">
        <div>
          <p className="eyebrow">Danh mục sản phẩm</p>
          <h1>Quản lý danh mục</h1>
          <p>Quản trị cấu trúc cây danh mục thời trang, phân cấp cha - con, đường dẫn slug thân thiện và trạng thái hiển thị trên cửa hàng.</p>
        </div>
        <button type="button" onClick={startCreate}>+ Thêm danh mục</button>
      </div>

      <div className="category-admin-kpi-grid">
        <CategoryKpi label="Tổng danh mục" value={summary.total} />
        <CategoryKpi label="Đang hiển thị" value={summary.active} tone="success" />
        <CategoryKpi label="Tạm ẩn" value={summary.inactive} tone="warning" />
        <CategoryKpi label="Danh mục gốc" value={summary.rootCount} />
        <CategoryKpi label="Có sản phẩm" value={summary.withProducts} />
      </div>

      <div className="category-admin-toolbar">
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
        <select value={hierarchyFilter} onChange={(event) => setHierarchyFilter(event.target.value)}>
          <option value="ALL">Tất cả cấp bậc</option>
          <option value="ROOT">Danh mục gốc</option>
          <option value="CHILD">Danh mục con</option>
        </select>
        <button type="button" className="ghost" onClick={loadData} disabled={loading}>
          {loading ? 'Đang tải...' : 'Làm mới'}
        </button>
      </div>

      <div className="category-admin-layout">
        <div className="category-admin-list-panel">
          <div className="category-admin-panel-heading">
            <div>
              <p className="eyebrow">Danh sách</p>
              <h2>{filteredCategories.length} danh mục</h2>
            </div>
            <span>{products.length} sản phẩm trong catalog</span>
          </div>

          {loading && <div className="admin-product-loading" />}
          {!loading && filteredCategories.length === 0 && (
            <p className="empty-state">Chưa có danh mục nào phù hợp với bộ lọc.</p>
          )}

          <div className="category-admin-grid">
            {!loading && filteredCategories.map((cat) => (
              <CategoryCard
                key={cat.id}
                category={cat}
                stats={categoryStats[cat.id]}
                childCount={childrenMap[cat.id] || 0}
                selected={editingCategory?.id === cat.id}
                onEdit={startEdit}
                onAddChild={startCreateChild}
                onRemove={removeCategory}
              />
            ))}
          </div>
        </div>

        <aside className="category-admin-editor-panel">
          {editingCategory ? (
            <CategoryForm
              value={editingCategory}
              availableParents={availableParents}
              saving={saving}
              onChange={updateDraft}
              onSubmit={submit}
              onCancel={() => setEditingCategory(null)}
            />
          ) : (
            <div className="category-admin-empty-editor">
              <p className="eyebrow">Biểu mẫu</p>
              <h2>Chọn danh mục để chỉnh sửa</h2>
              <p>Hoặc tạo danh mục mới để phân loại và sắp xếp sản phẩm trong hệ thống thời trang.</p>
              <button type="button" onClick={startCreate}>Thêm danh mục mới</button>
            </div>
          )}
        </aside>
      </div>
    </section>
  )
}

function CategoryKpi({ label, value, tone = '' }) {
  return (
    <article className={`category-admin-kpi ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function CategoryCard({
  category,
  stats,
  childCount,
  selected,
  onEdit,
  onAddChild,
  onRemove,
}) {
  const icon = getCategoryIcon(category.name, category.slug)

  return (
    <article className={`category-admin-card ${selected ? 'selected' : ''}`}>
      <div className="category-admin-icon" title={category.name}>
        <span>{icon}</span>
      </div>

      <div className="category-admin-card-body">
        <div className="category-admin-card-head">
          <div>
            <p className="eyebrow">#{category.id} · {category.slug}</p>
            <h2>{category.name}</h2>
          </div>
          <span className={`category-admin-status ${category.isActive ? 'active' : 'inactive'}`}>
            {category.isActive ? 'Đang hiển thị' : 'Tạm ẩn'}
          </span>
        </div>

        <div className="category-admin-card-badges">
          {category.parent?.name ? (
            <span className="category-admin-badge parent" title={`Thuộc danh mục #${category.parent.id}`}>
              ↳ {category.parent.name}
            </span>
          ) : (
            <span className="category-admin-badge">📁 Danh mục gốc</span>
          )}

          {childCount > 0 && (
            <span className="category-admin-badge children">
              📂 {childCount} mục con
            </span>
          )}
        </div>

        <p>{category.description || 'Chưa có mô tả cho danh mục này.'}</p>

        <div className="category-admin-card-meta">
          <span>{Number(stats?.total || 0)} sản phẩm</span>
          <span>{Number(stats?.active || 0)} đang bán</span>
          <span>{Number(stats?.stock || 0)} tồn kho</span>
        </div>

        <div className="category-admin-actions">
          <button type="button" className="ghost" onClick={() => onAddChild(category)} title="Thêm danh mục con thuộc danh mục này">
            + Mục con
          </button>
          <button type="button" onClick={() => onEdit(category)}>Sửa</button>
          <button type="button" className="danger" onClick={() => onRemove(category)}>Xóa</button>
        </div>
      </div>
    </article>
  )
}

function CategoryForm({
  value,
  availableParents,
  saving,
  onChange,
  onSubmit,
  onCancel,
}) {
  function updateName(name) {
    // Nếu chưa có slug hoặc đang tạo mới thì tự sinh slug
    const currentSlugIsAuto = !value.slug || value.slug === slugify(value.name)
    onChange({
      name,
      slug: currentSlugIsAuto ? slugify(name) : value.slug,
    })
  }

  const previewIcon = getCategoryIcon(value.name, value.slug)
  const parentObj = availableParents.find((p) => String(p.id) === String(value.parentId))

  return (
    <form className="category-admin-form" onSubmit={onSubmit}>
      <div className="category-admin-panel-heading">
        <div>
          <p className="eyebrow">Biểu mẫu</p>
          <h2>{value.id ? `Sửa danh mục #${value.id}` : 'Thêm danh mục'}</h2>
        </div>
        <span className={`category-admin-status ${value.isActive ? 'active' : 'inactive'}`}>
          {value.isActive ? 'Đang hiển thị' : 'Tạm ẩn'}
        </span>
      </div>

      <div className="category-admin-preview">
        <div className="category-admin-icon large">
          <span>{previewIcon}</span>
        </div>
        <div>
          <strong>{value.name || 'Tên danh mục'}</strong>
          <small>{value.slug ? `/${value.slug}` : '/slug-danh-muc'}</small>
          {parentObj && <small style={{ color: '#2b5580', marginTop: '2px' }}>↳ Thuộc: {parentObj.name}</small>}
        </div>
      </div>

      <label>Tên danh mục *
        <input
          value={value.name}
          onChange={(event) => updateName(event.target.value)}
          placeholder="VD: Áo sơ mi lụa, Quần tây..."
          required
        />
      </label>

      <label>Đường dẫn Slug *
        <input
          value={value.slug}
          onChange={(event) => onChange({ slug: slugify(event.target.value) })}
          placeholder="VD: ao-so-mi-lua"
          required
        />
      </label>

      <label>Danh mục cha
        <select
          value={value.parentId}
          onChange={(event) => onChange({ parentId: event.target.value })}
        >
          <option value="">-- Không có (Là danh mục gốc) --</option>
          {availableParents.map((parent) => (
            <option key={parent.id} value={parent.id}>
              {parent.name} (#{parent.id})
            </option>
          ))}
        </select>
      </label>

      <label>Mô tả danh mục
        <textarea
          value={value.description}
          onChange={(event) => onChange({ description: event.target.value })}
          placeholder="Mô tả phong cách, định vị nhóm sản phẩm thuộc danh mục này..."
          rows={3}
        />
      </label>

      <label className="category-admin-toggle">
        <input
          type="checkbox"
          checked={value.isActive}
          onChange={(event) => onChange({ isActive: event.target.checked })}
        />
        <span>Hiển thị danh mục trên cửa hàng</span>
      </label>

      <div className="category-admin-submit-row">
        <button type="button" className="ghost" onClick={onCancel} disabled={saving}>Hủy</button>
        <button type="submit" disabled={saving}>
          {saving ? 'Đang lưu...' : value.id ? 'Lưu thay đổi' : 'Tạo danh mục'}
        </button>
      </div>
    </form>
  )
}

function toCategoryPayload(cat) {
  return {
    name: cat.name.trim(),
    slug: slugify(cat.slug || cat.name),
    description: cat.description?.trim() || null,
    isActive: Boolean(cat.isActive),
    parent: cat.parentId ? { id: Number(cat.parentId) } : null,
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

function getCategoryIcon(name = '', slug = '') {
  const text = `${name} ${slug}`.toLowerCase()
  if (text.includes('áo khoác') || text.includes('jacket') || text.includes('outerwear') || text.includes('blazer')) return '🧥'
  if (text.includes('áo thun') || text.includes('ao thun') || text.includes('t-shirt') || text.includes('tee')) return '👕'
  if (text.includes('sơ mi') || text.includes('so mi') || text.includes('shirt') || text.includes('áo')) return '👔'
  if (text.includes('quần') || text.includes('quan') || text.includes('pant') || text.includes('jean') || text.includes('trouser')) return '👖'
  if (text.includes('đầm') || text.includes('dam') || text.includes('váy') || text.includes('vay') || text.includes('dress') || text.includes('skirt')) return '👗'
  if (text.includes('phụ kiện') || text.includes('phu kien') || text.includes('túi') || text.includes('tui') || text.includes('bag')) return '👜'
  if (text.includes('giày') || text.includes('giay') || text.includes('sneaker') || text.includes('shoe')) return '👟'
  if (text.includes('nón') || text.includes('mũ') || text.includes('hat') || text.includes('cap')) return '🧢'
  if (text.includes('trang sức') || text.includes('nhẫn') || text.includes('dây chuyền')) return '💍'
  return '🏷️'
}
