export function ProductFilters({
  query,
  filters,
  categories = [],
  sort,
  onQueryChange,
  onFiltersChange,
  onFilterFieldChange,
  onSortChange,
  onReset,
  onSubmit,
}) {
  const childParentIds = new Set(categories.map((cat) => cat.parent?.id).filter(Boolean))
  const categoryOptions = categories.length > 0
    ? categories.filter((cat) => cat.isActive !== false && (cat.parent?.id || !childParentIds.has(cat.id)))
    : [
        { name: 'Áo' },
        { name: 'Quần' },
        { name: 'Đầm' },
        { name: 'Phụ kiện' },
      ]

  return (
    <form className="product-toolbar" onSubmit={onSubmit}>
      <div className="search-field">
        <input
          placeholder="Tên, chất liệu, phong cách..."
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
        />
      </div>

      <select value={filters.category} onChange={(event) => onFiltersChange({ ...filters, category: event.target.value })}>
        <option value="">Tất cả danh mục</option>
        {categoryOptions.map((cat) => (
          <option key={cat.id || cat.name} value={cat.name}>
            {cat.parent?.name ? `${cat.parent.name} · ${cat.name}` : cat.name}
          </option>
        ))}
      </select>

      <input
        placeholder="Size"
        value={filters.size}
        onChange={(event) => onFilterFieldChange({ ...filters, size: event.target.value })}
      />
      <input
        placeholder="Màu sắc"
        value={filters.color}
        onChange={(event) => onFilterFieldChange({ ...filters, color: event.target.value })}
      />
      <input
        inputMode="numeric"
        placeholder="Giá từ"
        value={filters.minPrice}
        onChange={(event) => onFilterFieldChange({ ...filters, minPrice: event.target.value })}
      />

      <input
        inputMode="numeric"
        placeholder="Giá đến"
        value={filters.maxPrice}
        onChange={(event) => onFilterFieldChange({ ...filters, maxPrice: event.target.value })}
      />

      <select value={sort} onChange={(event) => onSortChange(event.target.value)}>
        <option value="featured">Nổi bật</option>
        <option value="newest">Mới nhất</option>
        <option value="price-asc">Giá tăng</option>
        <option value="price-desc">Giá giảm</option>
        <option value="rating">Đánh giá</option>
      </select>

      <button type="submit">Tìm kiếm</button>
      <button type="button" className="ghost" onClick={onReset}>Xóa lọc</button>
    </form>
  )
}
