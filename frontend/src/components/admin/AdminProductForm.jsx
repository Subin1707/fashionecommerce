export function AdminProductForm({
  value,
  onChange,
  onSubmit,
  title = 'Thêm sản phẩm',
  submitLabel = 'Tạo sản phẩm',
  brands = [],
  categories = [],
  saving = false,
}) {
  const images = Array.isArray(value.images) ? value.images : []

  function updateField(field, fieldValue) {
    onChange({ ...value, [field]: fieldValue })
  }

  function addImage() {
    onChange({
      ...value,
      images: [
        ...images,
        {
          imageUrl: '',
          altText: value.name || '',
          isPrimary: images.length === 0,
          displayOrder: images.length + 1,
        },
      ],
    })
  }

  function updateImage(index, field, fieldValue) {
    const nextImages = images.map((image, imageIndex) => {
      if (field === 'isPrimary') {
        return { ...image, isPrimary: imageIndex === index ? fieldValue : false }
      }
      return imageIndex === index ? { ...image, [field]: fieldValue } : image
    })
    onChange({ ...value, images: nextImages })
  }

  function removeImage(index) {
    const nextImages = images.filter((_, imageIndex) => imageIndex !== index)
    if (nextImages.length > 0 && !nextImages.some((image) => image.isPrimary)) {
      nextImages[0] = { ...nextImages[0], isPrimary: true }
    }
    onChange({ ...value, images: nextImages })
  }

  return (
    <form className="admin-product-form" onSubmit={onSubmit}>
      <div className="admin-product-form-heading">
        <div>
          <p className="eyebrow">Biểu mẫu sản phẩm</p>
          <h2>{title}</h2>
        </div>
        <span>{value.status || 'ACTIVE'}</span>
      </div>

      <div className="admin-product-form-grid">
        <label className="admin-product-wide-field">Tên sản phẩm
          <input
            value={value.name}
            onChange={(event) => updateField('name', event.target.value)}
            placeholder="Áo khoác denim lightwash"
            required
          />
        </label>

        <label>Thương hiệu
          <select
            value={value.brandId}
            onChange={(event) => updateField('brandId', event.target.value)}
            required
          >
            <option value="">Chọn thương hiệu</option>
            {brands.filter((brand) => brand.isActive !== false).map((brand) => (
              <option key={brand.id} value={brand.id}>{brand.name}</option>
            ))}
          </select>
        </label>

        <label>Danh mục
          <select
            value={value.categoryId}
            onChange={(event) => updateField('categoryId', event.target.value)}
            required
          >
            <option value="">Chọn danh mục</option>
            {categories.filter((category) => category.isActive !== false).map((category) => (
              <option key={category.id} value={category.id}>{category.name}</option>
            ))}
          </select>
        </label>

        <label>Giá niêm yết
          <input
            type="number"
            min="0"
            value={value.basePrice}
            onChange={(event) => updateField('basePrice', event.target.value)}
            placeholder="860000"
            required
          />
        </label>

        <label>Giá khuyến mãi
          <input
            type="number"
            min="0"
            value={value.salePrice}
            onChange={(event) => updateField('salePrice', event.target.value)}
            placeholder="Có thể để trống"
          />
        </label>

        <label className="checkbox-row admin-product-wide-field">
          <input
            type="checkbox"
            checked={Boolean(value.hasVariants)}
            onChange={(event) =>
              updateField('hasVariants', event.target.checked)
            }
          />
          Sản phẩm có nhiều biến thể
        </label>

        {!value.hasVariants ? (
          <label>
            Số lượng tồn kho
            <input
              type="number"
              min="0"
              step="1"
              value={value.stockQty ?? 0}
              onChange={(event) =>
                updateField('stockQty', event.target.value)
              }
              required
            />
          </label>
        ) : (
          <div className="variant-section admin-product-wide-field">
            <h3>Biến thể & tồn kho</h3>

            {(value.variants || []).map((variant, index) => (
              <div className="variant-row" key={index}>
                <input
                  type="text"
                  placeholder="Màu"
                  value={variant.color ?? ''}
                  onChange={(event) => {
                    const next = [...(value.variants || [])]
                    next[index] = {
                      ...next[index],
                      color: event.target.value,
                    }
                    updateField('variants', next)
                  }}
                />

                <input
                  type="text"
                  placeholder="Size"
                  value={variant.size ?? ''}
                  onChange={(event) => {
                    const next = [...(value.variants || [])]
                    next[index] = {
                      ...next[index],
                      size: event.target.value,
                    }
                    updateField('variants', next)
                  }}
                />

                <input
                  type="number"
                  min="0"
                  step="1"
                  placeholder="Tồn kho"
                  value={variant.stockQty ?? 0}
                  onChange={(event) => {
                    const next = [...(value.variants || [])]
                    next[index] = {
                      ...next[index],
                      stockQty: event.target.value,
                    }
                    updateField('variants', next)
                  }}
                />

                <input
                  type="number"
                  min="0"
                  placeholder="Giá cộng thêm"
                  value={variant.priceAdjustment ?? 0}
                  onChange={(event) => {
                    const next = [...(value.variants || [])]
                    next[index] = {
                      ...next[index],
                      priceAdjustment: event.target.value,
                    }
                    updateField('variants', next)
                  }}
                />

                <button
                  type="button"
                  className="danger"
                  onClick={() => {
                    const next = (value.variants || []).filter(
                      (_, variantIndex) => variantIndex !== index,
                    )
                    updateField('variants', next)
                  }}
                >
                  Xóa
                </button>
              </div>
            ))}

            <button
              type="button"
              className="ghost"
              onClick={() =>
                updateField('variants', [
                  ...(value.variants || []),
                  {
                    color: '',
                    size: '',
                    stockQty: 0,
                    priceAdjustment: 0,
                  },
                ])
              }
            >
              + Thêm biến thể
            </button>
          </div>
        )}

        <label>Chất liệu
          <input
            value={value.material}
            onChange={(event) => updateField('material', event.target.value)}
            placeholder="Denim 12oz"
          />
        </label>

        <label>Kiểu dáng
          <input
            value={value.fit}
            onChange={(event) => updateField('fit', event.target.value)}
            placeholder="Regular, Slim, Boxy..."
          />
        </label>

        <label>Đối tượng
          <select
            value={value.gender}
            onChange={(event) => updateField('gender', event.target.value)}
          >
            <option>UNISEX</option>
            <option>FEMALE</option>
            <option>MALE</option>
            <option>WOMEN</option>
            <option>MEN</option>
          </select>
        </label>

        <label>Trạng thái
          <select
            value={value.status}
            onChange={(event) => updateField('status', event.target.value)}
          >
            <option>ACTIVE</option>
            <option>INACTIVE</option>
          </select>
        </label>

        <label className="admin-product-wide-field">Mô tả
          <textarea
            value={value.description}
            onChange={(event) => updateField('description', event.target.value)}
            placeholder="Mô tả chất liệu, form dáng và dịp sử dụng..."
          />
        </label>
      </div>

      <div className="admin-product-toggle-grid">
        <label>
          <input
            type="checkbox"
            checked={Boolean(value.isFeatured)}
            onChange={(event) => updateField('isFeatured', event.target.checked)}
          />
          <span>Nổi bật</span>
          <small>Ưu tiên hiển thị trong catalog.</small>
        </label>
        <label>
          <input
            type="checkbox"
            checked={Boolean(value.isNew)}
            onChange={(event) => updateField('isNew', event.target.checked)}
          />
          <span>Hàng mới</span>
          <small>Gắn nhãn mới trên thẻ sản phẩm.</small>
        </label>
      </div>

      <section className="admin-product-image-panel">
        <div className="admin-product-image-heading">
          <div>
            <p className="eyebrow">Hình ảnh sản phẩm</p>
            <h3>Ảnh hiển thị trên catalog</h3>
          </div>
          <button type="button" className="ghost" onClick={addImage}>+ Thêm ảnh</button>
        </div>

        {images.length === 0 ? (
          <p className="admin-product-image-empty">Chưa có ảnh. Thêm URL ảnh để khách hàng xem được sản phẩm ở trang chủ, giỏ hàng và chi tiết.</p>
        ) : (
          <div className="admin-product-image-list">
            {images.map((image, index) => (
              <div className="admin-product-image-row" key={`${image.id || 'new'}-${index}`}>
                <div className="admin-product-image-preview">
                  {image.imageUrl ? <img src={image.imageUrl} alt={image.altText || value.name || 'Ảnh sản phẩm'} /> : <span>Ảnh</span>}
                </div>

                <div className="admin-product-image-fields">
                  <label className="admin-product-image-url-field">URL ảnh
                    <input
                      value={image.imageUrl || ''}
                      onChange={(event) => updateImage(index, 'imageUrl', event.target.value)}
                      placeholder="https://... hoặc /images/san-pham.jpg"
                    />
                  </label>

                  <label>Văn bản thay thế
                    <input
                      value={image.altText || ''}
                      onChange={(event) => updateImage(index, 'altText', event.target.value)}
                      placeholder={value.name || 'Tên sản phẩm'}
                    />
                  </label>

                  <label>Thứ tự
                    <input
                      type="number"
                      min="0"
                      value={image.displayOrder ?? index + 1}
                      onChange={(event) => updateImage(index, 'displayOrder', event.target.value)}
                    />
                  </label>

                  <label className="admin-product-primary-toggle">
                    <input
                      type="checkbox"
                      checked={Boolean(image.isPrimary)}
                      onChange={(event) => updateImage(index, 'isPrimary', event.target.checked)}
                    />
                    <span>Ảnh chính</span>
                  </label>
                </div>

                <button type="button" className="danger" onClick={() => removeImage(index)}>Xóa ảnh</button>
              </div>
            ))}
          </div>
        )}
      </section>

      <div className="admin-product-submit-row">
        <button type="submit" disabled={saving}>{saving ? 'Đang lưu...' : submitLabel}</button>
      </div>
    </form>
  )
}
