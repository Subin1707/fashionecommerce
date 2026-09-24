import { useEffect, useState } from 'react'
import { ProductCard } from './ProductCard'
import * as productService from '../../services/productService'

export function SimilarProducts({ productId, onAdd, onView, onToggleWishlist, isWishlisted, isTogglingWishlist }) {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    productService.getSimilarProducts(productId)
      .then((rows) => { if (!cancelled) setProducts(Array.isArray(rows) ? rows : []) })
      .catch(() => { if (!cancelled) setProducts([]) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [productId])

  if (loading) return <section className="similar-products"><p className="empty-state">Đang tìm sản phẩm tương tự...</p></section>
  if (!products.length) return null

  return (
    <section className="similar-products">
      <div className="similar-products-heading">
        <div>
          <p className="eyebrow">Bạn có thể thích</p>
          <h2>Sản phẩm tương tự</h2>
        </div>
        <span>{products.length} lựa chọn</span>
      </div>
      <div className="similar-products-grid">
        {products.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            onAdd={onAdd}
            onView={onView}
            onToggleWishlist={onToggleWishlist}
            isWishlisted={isWishlisted(product.id)}
            isTogglingWishlist={isTogglingWishlist(product.id)}
          />
        ))}
      </div>
    </section>
  )
}
