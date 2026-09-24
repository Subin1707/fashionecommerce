import { formatCurrency } from '../../utils/formatCurrency'

export function ProductCard({
  product,
  selected,
  onSelect,
  onAdd,
  onBuyNow,
  onView,
  onToggleWishlist,
  isWishlisted = false,
  isTogglingWishlist = false,
  isAdding = false,
  isBuying = false,
}) {
  const price = product.salePrice || product.basePrice
  const hasSale = product.salePrice && product.basePrice && product.salePrice < product.basePrice
  const totalStock = product.totalStock ?? product.variants?.reduce((sum, item) => sum + Number(item.stockQty || 0), 0)
  const disabled = isAdding || isBuying || totalStock === 0

  return (
    <article className={`product-card ${selected ? 'selected' : ''}`} onClick={() => onSelect?.(product)}>
      <div className="product-image">
        {product.primaryImageUrl ? (
          <img src={product.primaryImageUrl} alt={product.name} loading="lazy" />
        ) : (
          <span>{product.categoryName?.slice(0, 2) || 'SP'}</span>
        )}
        {onToggleWishlist && <button
          type="button"
          className={`wishlist-toggle ${isWishlisted ? 'active' : ''}`}
          aria-label={isWishlisted ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'}
          aria-pressed={isWishlisted}
          disabled={isTogglingWishlist}
          onClick={(event) => { event.stopPropagation(); onToggleWishlist(product) }}
        >{isWishlisted ? '♥' : '♡'}</button>}
      </div>

      <div className="card-row">
        <span>{product.brandName || 'Thương hiệu'}</span>
        {product.isNew && <mark>Mới</mark>}
      </div>

      <h2>{product.name}</h2>
      <p>{product.material || 'Chất liệu'} - {product.fit || 'Form chuẩn'}</p>

      <div className="price-row">
        <strong>{formatCurrency(price)}</strong>
        {hasSale && <del>{formatCurrency(product.basePrice)}</del>}
      </div>

      <div className="mini-meta">
        <span>{totalStock > 0 ? `${totalStock} sản phẩm` : 'Hết hàng'}</span>
        <span>{product.averageRating || 0}/5</span>
      </div>

      <div className="chips compact">
        {(product.colors || []).slice(0, 3).map((color) => <span key={color}>{color}</span>)}
        {(product.sizes || []).slice(0, 4).map((size) => <span key={size}>{size}</span>)}
      </div>

      <button
        type="button"
        className="product-detail-link"
        onClick={(event) => { event.stopPropagation(); onView?.(product) }}
      >
        <span>Xem chi tiết</span>
        <span aria-hidden="true">→</span>
      </button>

      <div className="product-card-actions">
        <button
          type="button"
          className="product-add-cart-button"
          disabled={disabled}
          onClick={(event) => { event.stopPropagation(); onAdd(product) }}
        >
          {isAdding ? 'Đang thêm...' : 'Thêm giỏ'}
        </button>
        {onBuyNow && (
          <button
            type="button"
            className="product-buy-now-button"
            disabled={disabled}
            onClick={(event) => { event.stopPropagation(); onBuyNow(product) }}
          >
            {isBuying ? 'Đang mua...' : 'Mua ngay'}
          </button>
        )}
      </div>
    </article>
  )
}
