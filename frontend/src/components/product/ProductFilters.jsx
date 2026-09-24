export function ProductFilters({
  query,
  filters,
  categories = [],
  sort,
  onQueryChange,
  onFiltersChange,
  onSortChange,
  onReset,
  onSubmit,
}) {
  const categoryOptions = categories.length > 0
    ? categories.filter((cat) => cat.isActive !== false)
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
            {cat.name}
          </option>
        ))}
      </select>

      <select value={filters.size} onChange={(event) => onFiltersChange({ ...filters, size: event.target.value })}>
        <option value="">Size</option>
        <option>S</option>
        <option>M</option>
        <option>L</option>
        <option>XL</option>
        <option>28</option>
        <option>30</option>
        <option>32</option>
      </select>

      <input
        placeholder="Màu sắc"
        value={filters.color}
        onChange={(event) => onFiltersChange({ ...filters, color: event.target.value })}
      />

      <input
        inputMode="numeric"
        placeholder="Giá từ"
        value={filters.minPrice}
        onChange={(event) => onFiltersChange({ ...filters, minPrice: event.target.value })}
      />

      <input
        inputMode="numeric"
        placeholder="Giá đến"
        value={filters.maxPrice}
        onChange={(event) => onFiltersChange({ ...filters, maxPrice: event.target.value })}
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
