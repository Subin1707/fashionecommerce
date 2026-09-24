import { useEffect, useMemo, useState } from 'react'
import { formatCurrency } from '../../utils/formatCurrency'
import * as productService from '../../services/productService'
import { selectProductVariant } from '../../utils/selectProductVariant'

export function ProductDetail({ product, onAdd, onBuyNow, isAdding = false, isBuying = false, onBack, onToggleWishlist, isWishlisted = false, isTogglingWishlist = false }) {
  const [fetchedVariants, setFetchedVariants] = useState([])
  const [fetchedProductId, setFetchedProductId] = useState(null)
  const [selection, setSelection] = useState(null)
  const [selectedImage, setSelectedImage] = useState('')
  const [quantity, setQuantity] = useState(1)

  useEffect(() => {
    let cancelled = false

    if (!product?.id || product?.variants?.length) return undefined

    productService.getProductVariants(product.id)
      .then((rows) => {
        if (cancelled) return
        setFetchedProductId(product.id)
        setFetchedVariants(rows)
      })
      .catch(() => {
        if (!cancelled) {
          setFetchedProductId(product.id)
          setFetchedVariants([])
        }
      })

    return () => {
      cancelled = true
    }
  }, [product])

  const variants = product?.variants?.length || fetchedProductId !== product?.id
    ? product?.variants || []
    : fetchedVariants

  const defaultVariant = variants.find((item) => Number(item.stockQty) > 0) || variants[0]
  const selectedColor = selection ? selection.color : defaultVariant?.color
  const selectedSize = selection ? selection.size : defaultVariant?.size
  const selectedVariant = selectProductVariant(variants, { color: selectedColor, size: selectedSize })

  const basePrice = Number(product.salePrice || product.basePrice || 0)
  const variantPrice = selectedVariant?.priceAdjustment
    ? basePrice + Number(selectedVariant.priceAdjustment)
    : basePrice
  const hasSale = Number(product.salePrice || 0) > 0 && Number(product.salePrice) < Number(product.basePrice || 0)
  const maxQuantity = Math.max(0, Number(selectedVariant?.stockQty ?? 0))
  const canAdd = Boolean(onAdd && selectedVariant && maxQuantity > 0)
  const galleryImages = useMemo(() => {
    const urls = [
      product.primaryImageUrl,
      ...(product.imageUrls || []),
      ...(product.images || []).map((image) => image.imageUrl),
    ]
      .filter(Boolean)
    return [...new Set(urls)]
  }, [product])
  const heroImage = galleryImages.includes(selectedImage) ? selectedImage : galleryImages[0]

  const sizes = [...new Set(variants.map((variant) => variant.size).filter(Boolean))]
  const colors = [...new Set(variants.map((variant) => variant.color).filter(Boolean))]
  const categoryLabel = product.categoryName || 'Ready to wear'
  const stockTone = maxQuantity > 8 ? 'available' : maxQuantity > 0 ? 'low' : 'soldout'

  function chooseVariantBy(attribute, value) {
    setSelection((current) => ({
      color: selectedColor,
      size: selectedSize,
      ...current,
      [attribute]: value,
    }))
    setQuantity(1)
  }

  function changeQuantity(delta) {
    setQuantity((current) => {
      const next = current + delta
      if (maxQuantity <= 0) return 1
      return Math.min(maxQuantity, Math.max(1, next))
    })
  }

  return (
    <div className="product-detail-experience">
      <section className="product-detail-gallery">
        <button type="button" className="detail-back-button" onClick={onBack}>
          Quay lại
        </button>

        <div className="detail-hero-image">
          <div className="detail-image-badges">
            {product.isNew && <span>Mới</span>}
            {hasSale && <span>Ưu đãi</span>}
          </div>
          {heroImage ? (
            <img src={heroImage} alt={product.name} />
          ) : (
            <div className="detail-image-fallback">
              <strong>{product.categoryName || 'Fashion'}</strong>
              <span>{product.name}</span>
            </div>
          )}
        </div>

        <div className="detail-thumbnail-row">
          {(galleryImages.length ? galleryImages : [null, null, null]).slice(0, 4).map((image, index) => (
            <button
              key={image || index}
              type="button"
              className={`detail-thumbnail ${image === heroImage ? 'active' : ''}`}
              onClick={() => image && setSelectedImage(image)}
            >
              {image ? <img src={image} alt={`${product.name} ${index + 1}`} /> : <span>{index + 1}</span>}
            </button>
          ))}
        </div>
      </section>

      <section className="detail-buy-panel">
        <div className="detail-product-heading">
          <div className="detail-kicker-row">
            <p className="eyebrow">{product.brandName || 'Thương hiệu'}</p>
            <span>{categoryLabel}</span>
          </div>
          <div className="detail-title-row">
            <h1>{product.name}</h1>
            {onToggleWishlist && <button
              type="button"
              className={`wishlist-toggle detail-wishlist-toggle ${isWishlisted ? 'active' : ''}`}
              aria-label={isWishlisted ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'}
              aria-pressed={isWishlisted}
              disabled={isTogglingWishlist}
              onClick={() => onToggleWishlist(product)}
            >{isWishlisted ? '♥' : '♡'}</button>}
          </div>
          <p>{product.shortDescription || product.description || `${product.material || 'Chất liệu cao cấp'} cho tủ đồ hằng ngày.`}</p>
        </div>

        <div className="detail-price-row">
          <strong>{formatCurrency(variantPrice)}</strong>
          {hasSale && <del>{formatCurrency(product.basePrice)}</del>}
          {hasSale && <mark>Sale</mark>}
        </div>

        <div className="detail-rating-row">
          <span>{product.averageRating || 0}/5</span>
          <span>{product.totalReviews || 0} đánh giá</span>
          <span className={`detail-stock-pill ${stockTone}`} aria-live="polite">{!selectedVariant ? 'Không có tổ hợp màu / size này' : maxQuantity > 0 ? `Còn ${maxQuantity} sản phẩm` : 'Hết hàng'}</span>
        </div>

        {colors.length > 0 && (
          <div className="detail-option-group">
            <div className="detail-option-title">Màu sắc</div>
            <div className="detail-swatch-row">
              {colors.map((color) => (
                <button
                  key={color}
                  type="button"
                  className={selectedColor === color ? 'active' : ''}
                  aria-pressed={selectedColor === color}
                  onClick={() => chooseVariantBy('color', color)}
                >
                  {color}
                </button>
              ))}
            </div>
          </div>
        )}

        {sizes.length > 0 && (
          <div className="detail-option-group">
            <div className="detail-option-title">Kích cỡ</div>
            <div className="detail-size-row">
              {sizes.map((size) => (
                <button
                  key={size}
                  type="button"
                  className={selectedSize === size ? 'active' : ''}
                  aria-pressed={selectedSize === size}
                  onClick={() => chooseVariantBy('size', size)}
                >
                  {size}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="detail-purchase-row">
          <div className="detail-quantity-stepper">
            <button type="button" onClick={() => changeQuantity(-1)} disabled={quantity <= 1}>-</button>
            <span>{quantity}</span>
            <button type="button" onClick={() => changeQuantity(1)} disabled={quantity >= maxQuantity}>+</button>
          </div>

          <div className="detail-action-pair">
            <button
              type="button"
              className="detail-add-button"
              disabled={isAdding || isBuying || !canAdd}
              onClick={() => onAdd(product, selectedVariant, quantity)}
            >
              {isAdding ? 'Đang thêm...' : 'Thêm vào giỏ'}
            </button>

            {onBuyNow && (
              <button
                type="button"
                className="detail-buy-now-button"
                disabled={isAdding || isBuying || !canAdd}
                onClick={() => onBuyNow(product, selectedVariant, quantity)}
              >
                {isBuying ? 'Đang xử lý...' : 'Mua ngay'}
              </button>
            )}
          </div>
        </div>

        {variants.length > 0 && (
          <div className="detail-selection-note">
            <span>Đang chọn</span>
            <strong>{selectedColor || 'Màu mặc định'} / {selectedSize || 'Size chuẩn'}</strong>
          </div>
        )}

        <dl className="detail-spec-grid">
          <div><dt>Chất liệu</dt><dd>{product.material || 'Đẳng cấp premium'}</dd></div>
          <div><dt>Form</dt><dd>{product.fit || 'Regular'}</dd></div>
          <div><dt>Phong cách</dt><dd>{product.gender || 'UNISEX'}</dd></div>
          <div><dt>SKU</dt><dd>{selectedVariant?.sku || 'Không có'}</dd></div>
        </dl>

        <div className="detail-service-list">
          <span><b>01</b> Freeship cho đơn từ 500.000 VND</span>
          <span><b>02</b> Đổi trả trong 7 ngày</span>
          <span><b>03</b> Thanh toán COD hoặc online an toàn</span>
        </div>
      </section>
    </div>
  )
}
