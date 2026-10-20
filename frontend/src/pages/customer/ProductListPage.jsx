import { useEffect, useMemo, useRef, useState } from 'react'
import { ProductCard } from '../../components/product/ProductCard'
import { ProductDetail } from '../../components/product/ProductDetail'
import { ProductFilters } from '../../components/product/ProductFilters'
import { useAuth } from '../../hooks/useAuth'
import { useCart } from '../../hooks/useCart'
import * as categoryService from '../../services/categoryService'
import * as productService from '../../services/productService'
import * as wishlistService from '../../services/wishlistService'
import { ROUTES } from '../../utils/constants'

const initialFilters = { categoryId: '', category: '', categoryName: '', size: '', color: '', minPrice: '', maxPrice: '' }
const fallbackCategories = [
  { slug: 'ao-thun', name: 'Áo thun' },
  { slug: 'quan', name: 'Quần' },
  { slug: 'ao-khoac', name: 'Áo khoác' },
  { slug: 'dam', name: 'Đầm' },
  { slug: 'phu-kien', name: 'Phụ kiện' },
]

export function ProductListPage({ setNotice, setRoute }) {
  const [query, setQuery] = useState('')
  const [filters, setFilters] = useState(initialFilters)
  const [categories, setCategories] = useState([])
  const [sort, setSort] = useState('featured')
  const [products, setProducts] = useState([])
  const [totalPages, setTotalPages] = useState(1)
  const [selectedProduct, setSelectedProduct] = useState(null)
  const [addingProductId, setAddingProductId] = useState(null)
  const [buyingProductId, setBuyingProductId] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const searchRequestId = useRef(0)
  const searchController = useRef(null)
  const currentFilters = useRef(initialFilters)
  const currentQuery = useRef('')
  const loadingMore = useRef(false)
  const [wishlistIds, setWishlistIds] = useState(new Set())
  const [togglingWishlistId, setTogglingWishlistId] = useState(null)
  const { isAdmin, isCustomer } = useAuth()
  const { addToCart } = useCart()

  useEffect(() => {
    if (!isCustomer) return
    wishlistService.getWishlist()
      .then((items) => setWishlistIds(new Set(items.map((item) => String(item.productId)))))
      .catch(() => setWishlistIds(new Set()))
  }, [isCustomer])

  useEffect(() => {
    loadProducts(initialFilters)
    categoryService.getCategories()
      .then((rows) => setCategories(Array.isArray(rows) ? rows : []))
      .catch(() => {})
    // oxlint-disable-next-line exhaustive-deps
  }, [])

  const sortedProducts = useMemo(() => {
    return [...products].sort((left, right) => {
      if (sort === 'price-asc') return priceOf(left) - priceOf(right)
      if (sort === 'price-desc') return priceOf(right) - priceOf(left)
      if (sort === 'rating') return Number(right.averageRating || 0) - Number(left.averageRating || 0)
      if (sort === 'newest') return Number(Boolean(right.isNew)) - Number(Boolean(left.isNew))
      return Number(Boolean(right.isFeatured)) - Number(Boolean(left.isFeatured))
    })
  }, [products, sort])
  const visibleCategories = categories.length ? categories : fallbackCategories

  async function loadProducts(extra = filters, keyword = query) {
    currentFilters.current = extra
    currentQuery.current = keyword
    const requestId = ++searchRequestId.current
    loadingMore.current = false
    searchController.current?.abort()
    const controller = new AbortController()
    searchController.current = controller
    setIsLoading(true)
    try {
      const result = await productService.searchProducts(
        { ...extra, keyword },
        { page: 0, size: 16, signal: controller.signal },
      )
      if (requestId !== searchRequestId.current) return
      setProducts(result.products)
      setTotalPages(result.totalPages)
      setNotice(`${result.products.length} sản phẩm đã tải.`)
    } catch (error) {
      if (requestId !== searchRequestId.current) return
      if (error.name === 'AbortError') return
      setNotice(`Chưa đọc được /api/search: ${error.message}`)
    } finally {
      if (requestId === searchRequestId.current) setIsLoading(false)
    }
  }

  async function loadMoreProducts() {
    if (loadingMore.current || isLoading || products.length === 0) return
    loadingMore.current = true
    const requestId = searchRequestId.current
    const controller = new AbortController()
    searchController.current = controller
    setIsLoading(true)
    try {
      const result = await productService.searchProducts(
        { ...filters, keyword: query },
        { page: Math.ceil(products.length / 16), size: 16, signal: controller.signal },
      )
      if (requestId !== searchRequestId.current) return
      setProducts((current) => [...current, ...result.products])
      setTotalPages(result.totalPages)
      setNotice(`${Math.min(products.length + result.products.length, result.totalElements ?? Infinity)} sản phẩm đã tải.`)
    } catch (error) {
      setNotice(`Chưa tải thêm được sản phẩm: ${error.message}`)
    } finally {
      loadingMore.current = false
      if (requestId === searchRequestId.current) setIsLoading(false)
    }
  }

  function resetFilters() {
    setQuery('')
    setFilters(initialFilters)
    currentQuery.current = ''
    currentFilters.current = initialFilters
    setSort('featured')
    loadProducts(initialFilters, '')
  }

  function handleSelectCategory(catName) {
    const nextFilters = {
      ...filters,
      categoryId: catName?.id ? String(catName.id) : '',
      category: catName?.slug || '',
      categoryName: catName?.name || '',
    }
    currentFilters.current = nextFilters
    setFilters(nextFilters)
    loadProducts(nextFilters)
  }

  function updateFilters(nextFilters) {
    currentFilters.current = nextFilters
    setFilters(nextFilters)
  }

  function updateQuery(nextQuery) {
    currentQuery.current = nextQuery
    setQuery(nextQuery)
  }

  async function handleAdd(product, variant) {
    if (isAdmin) {
      setNotice('Giỏ hàng chỉ dành cho khách hàng. Quản trị viên vui lòng sử dụng trang quản trị để vận hành sản phẩm.')
      return
    }

    if (!isCustomer) {
      setNotice('Vui lòng đăng nhập bằng tài khoản khách hàng (ví dụ: customer1@example.com) để thêm sản phẩm vào giỏ hàng.')
      setRoute?.(ROUTES.LOGIN)
      return
    }

    setAddingProductId(product.id)
    try {
      await addToCart(product, variant, 1)
      setNotice(`Đã thêm "${product.name}" vào giỏ hàng thành công!`)
    } catch (error) {
      setNotice(`Không thêm được sản phẩm vào giỏ hàng: ${error.message}`)
    } finally {
      setAddingProductId(null)
    }
  }

  async function handleToggleWishlist(product) {
    if (!isCustomer) {
      sessionStorage.setItem('fashion:wishlist-return-route', window.location.hash.replace(/^#/, '') || ROUTES.PRODUCTS)
      setNotice('Vui lòng đăng nhập bằng tài khoản Khách hàng để lưu sản phẩm yêu thích.')
      setRoute?.(ROUTES.LOGIN)
      return
    }

    const productKey = String(product.id)
    const isWishlisted = wishlistIds.has(productKey)
    setTogglingWishlistId(product.id)
    try {
      if (isWishlisted) {
        await wishlistService.removeFromWishlist(product.id)
        setWishlistIds((current) => new Set([...current].filter((id) => id !== productKey)))
        setNotice(`Đã bỏ "${product.name}" khỏi danh sách yêu thích.`)
      } else {
        await wishlistService.addToWishlist(product.id)
        setWishlistIds((current) => new Set(current).add(productKey))
        setNotice(`Đã thêm "${product.name}" vào sản phẩm yêu thích.`)
      }
    } catch (error) {
      setNotice(`Không cập nhật được yêu thích: ${error.message}`)
    } finally {
      setTogglingWishlistId(null)
    }
  }

  async function handleBuyNow(product, variant = null, quantity = 1) {
    if (isAdmin) {
      setNotice('Mua hàng và giỏ hàng chỉ dành cho khách hàng. Quản trị viên vui lòng đăng xuất để thử đặt hàng.')
      return
    }

    if (!isCustomer) {
      setNotice('Vui lòng đăng nhập bằng tài khoản Khách hàng để mua sản phẩm.')
      setRoute?.(ROUTES.LOGIN)
      return
    }

    setBuyingProductId(product.id)
    try {
      const selectedVariant = variant || await findBuyableVariant(product)
      if (!selectedVariant?.id) {
        throw new Error('Sản phẩm chưa có SKU hợp lệ.')
      }

      const basePrice = Number(product.salePrice || product.basePrice || 0)
      const unitPrice = selectedVariant.priceAdjustment
        ? basePrice + Number(selectedVariant.priceAdjustment)
        : basePrice

      sessionStorage.setItem('fashion:buyNowCheckout', JSON.stringify({
        productId: Number(product.id),
        variantId: Number(selectedVariant.id),
        productName: product.name,
        imageUrl: product.primaryImageUrl || product.imageUrls?.[0] || '',
        color: selectedVariant.color,
        size: selectedVariant.size,
        sku: selectedVariant.sku,
        quantity: Number(quantity) || 1,
        price: unitPrice,
      }))

      setRoute?.(ROUTES.CHECKOUT)
    } catch (error) {
      setNotice(`Không mua ngay được sản phẩm: ${error.message}`)
    } finally {
      setBuyingProductId(null)
    }
  }

  function handleView(product) {
    sessionStorage.setItem('fashion:lastProductDetail', JSON.stringify(product))
    setRoute?.(`${ROUTES.PRODUCT_DETAIL}?id=${encodeURIComponent(product.id)}`)
  }

  return (
    <div className="fashion-product-list-wrapper">
    <section className="store-grid upgraded-products">
      <div className="catalog-panel">
        <div className="catalog-summary">
          <div>
            <p className="eyebrow">Danh mục sản phẩm</p>
            <h2>Sản phẩm đang bán</h2>
          </div>
          <div className="summary-pills">
            <span>{sortedProducts.length} sản phẩm đã tải</span>
            <span>{totalStock(sortedProducts)} tồn kho</span>
          </div>
        </div>

        {visibleCategories.length > 0 && (
          <div className="catalog-category-pills">
            <button
              type="button"
              className={`category-pill-btn ${!filters.categoryId && !filters.category ? 'active' : ''}`}
              onClick={() => handleSelectCategory('')}
            >
              Tất cả
            </button>
            {visibleCategories.filter((category) => category.isActive !== false).map((cat) => (
              <button
                key={cat.id || cat.slug}
                type="button"
                className={`category-pill-btn ${cat.id
                  ? filters.categoryId === String(cat.id)
                  : filters.category === cat.slug ? 'active' : ''}`}
                onClick={() => handleSelectCategory(cat)}
              >
                {cat.parent?.name ? `${cat.parent.name} · ${cat.name}` : cat.name}
              </button>
            ))}
          </div>
        )}

        <ProductFilters
          query={query}
          filters={filters}
          categories={visibleCategories}
          sort={sort}
          onQueryChange={updateQuery}
          onFilterFieldChange={updateFilters}
          onSortChange={setSort}
          onReset={resetFilters}
          onSubmit={(event, appliedFilters, appliedQuery) => {
            event.preventDefault()
            loadProducts(appliedFilters, appliedQuery)
          }}
        />

        {isLoading && products.length === 0 && <p className="empty-state" role="status">Đang tải sản phẩm...</p>}
        {!isLoading && sortedProducts.length === 0 && <p className="empty-state">Chưa có sản phẩm phù hợp.</p>}
        {isLoading && products.length > 0 && <p className="catalog-loading-more" role="status">Đang cập nhật danh sách...</p>}

        <div className="product-grid" aria-busy={isLoading}>
          {sortedProducts.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              selected={selectedProduct?.id === product.id}
              onSelect={setSelectedProduct}
              onAdd={handleAdd}
              onBuyNow={handleBuyNow}
              onView={handleView}
              onToggleWishlist={handleToggleWishlist}
              isWishlisted={wishlistIds.has(String(product.id))}
              isTogglingWishlist={togglingWishlistId === product.id}
              isAdding={addingProductId === product.id}
              isBuying={buyingProductId === product.id}
            />
          ))}
        </div>
        {!isLoading && products.length > 0 && Math.ceil(products.length / 16) < totalPages && (
          <button type="button" className="catalog-load-more" onClick={loadMoreProducts}>
            Xem thêm sản phẩm
          </button>
        )}
      </div>

      {selectedProduct && (
        <ProductDetail
          key={selectedProduct.id}
          product={selectedProduct}
          onAdd={handleAdd}
          onBuyNow={handleBuyNow}
          onBack={() => setSelectedProduct(null)}
          onToggleWishlist={handleToggleWishlist}
          isWishlisted={wishlistIds.has(String(selectedProduct.id))}
          isTogglingWishlist={togglingWishlistId === selectedProduct.id}
          isAdding={addingProductId === selectedProduct.id}
          isBuying={buyingProductId === selectedProduct.id}
        />
      )}
    </section>
    </div>
  )
}

async function findBuyableVariant(product) {
  let variants = product.variants

  if (!Array.isArray(variants) || variants.length === 0) {
    variants = await productService.getProductVariants(product.id)
  }

  return variants.find((variant) => variant?.id && (variant.stockQty == null || Number(variant.stockQty) > 0))
    || variants[0]
}

function priceOf(product) {
  return Number(product.salePrice || product.basePrice || 0)
}

function totalStock(products) {
  return products.reduce((sum, product) => (
    sum + Number(product.totalStock || product.variants?.reduce((inner, variant) => inner + Number(variant.stockQty || 0), 0) || 0)
  ), 0)
}

