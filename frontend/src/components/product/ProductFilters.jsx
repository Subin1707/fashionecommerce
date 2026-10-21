export function ProductFilters({
  query,
  filters,
  categories = [],
  brands = [],
  sort,
  onQueryChange,
  onFilterFieldChange,
  onSortChange,
  onReset,
  onSubmit,
}) {
  const categoryOptions = categories.filter((category) => category.isActive !== false)
  const selectedCategory = categoryOptions.find((category) => (
    (category.id && filters.categoryId === String(category.id)) || filters.category === category.slug
  ))

  return (
    <form className="product-toolbar" onSubmit={(event) => {
      event.preventDefault()
      onSubmit(event, { ...filters }, query)
    }}>
      <div className="search-field">
        <input
          placeholder="Tên, chất liệu, phong cách..."
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
        />
      </div>

      <select
        aria-label="Danh mục sản phẩm"
        value={selectedCategory ? String(selectedCategory.id || `slug:${selectedCategory.slug}`) : ''}
        onChange={(event) => {
          const selectedValue = event.target.value
          const selected = categoryOptions.find((category) => String(category.id || `slug:${category.slug}`) === selectedValue)
          onFilterFieldChange({
            ...filters,
            categoryId: selected?.id ? String(selected.id) : '',
            category: selected?.slug || '',
            categoryName: selected?.name || '',
          })
        }}
      >
        <option value="">Tất cả danh mục</option>
        {categoryOptions.map((category) => (
          <option key={category.id || category.slug} value={String(category.id || `slug:${category.slug}`)}>
            {category.parent?.name ? `${category.parent.name} · ${category.name}` : category.name}
          </option>
        ))}
      </select>

      <select
        aria-label="Thương hiệu sản phẩm"
        value={filters.brandId || ''}
        onChange={(event) => {
          const brand = brands.find((item) => String(item.id) === event.target.value)
          onFilterFieldChange({ ...filters, brandId: brand ? String(brand.id) : '', brandName: brand?.name || '' })
        }}
      >
        <option value="">Tất cả thương hiệu</option>
        {brands.filter((brand) => brand.isActive !== false).map((brand) => (
          <option key={brand.id} value={String(brand.id)}>{brand.name}</option>
        ))}
      </select>

      <input
        aria-label="Kích cỡ"
        placeholder="Kích cỡ"
        value={filters.size}
        onChange={(event) => onFilterFieldChange({ ...filters, size: event.target.value })}
      />
      <input
        aria-label="Màu sắc"
        placeholder="Màu sắc"
        value={filters.color}
        onChange={(event) => onFilterFieldChange({ ...filters, color: event.target.value })}
      />
      <input
        aria-label="Giá từ"
        inputMode="numeric"
        placeholder="Giá từ"
        value={filters.minPrice}
        onChange={(event) => onFilterFieldChange({ ...filters, minPrice: event.target.value })}
      />
      <input
        aria-label="Giá đến"
        inputMode="numeric"
        placeholder="Giá đến"
        value={filters.maxPrice}
        onChange={(event) => onFilterFieldChange({ ...filters, maxPrice: event.target.value })}
      />

      <select aria-label="Sắp xếp sản phẩm" value={sort} onChange={(event) => onSortChange(event.target.value)}>
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
