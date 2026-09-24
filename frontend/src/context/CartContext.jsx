import { useCallback, useEffect, useMemo, useState } from 'react'
import { CartContext } from './cartContextStore'
import { useAuth } from '../hooks/useAuth'
import * as cartService from '../services/cartService'

export function CartProvider({ children }) {
  const { isCustomer } = useAuth()

  const [cart, setCart] = useState(null)
  const [cartLoading, setCartLoading] = useState(false)

  const loadCart = useCallback(async () => {
    if (!isCustomer) {
      setCart(null)
      return null
    }

    try {
      setCartLoading(true)
      const data = await cartService.getCart()
      setCart(data)
      return data
    } catch (error) {
      // 403/401: quyền bị từ chối → reset giỏ hàng im lặng, không crash
      const msg = error?.message || ''
      if (msg.includes('403') || msg.includes('401') || msg.includes('quyền') || msg.includes('đăng nhập')) {
        setCart(null)
        return null
      }
      console.error('Lỗi load cart:', error)
      setCart(null)
      return null
    } finally {
      setCartLoading(false)
    }
  }, [isCustomer])

  useEffect(() => {
    if (isCustomer) {
      Promise.resolve().then(loadCart)
    }
  }, [isCustomer, loadCart])

  // =============================
  // THÊM VÀO GIỎ
  // =============================
  const addToCart = useCallback(async function addToCart(product, selectedVariant = null, quantity = 1) {
    if (!isCustomer) {
      throw new Error(
        'Vui lòng đăng nhập bằng tài khoản Khách hàng để thêm vào giỏ. Tài khoản demo: customer1@example.com / Customer@123'
      )
    }

    if (!product?.id) {
      throw new Error('Không tìm thấy thông tin sản phẩm.')
    }

    let variant = selectedVariant

    if (!variant || !variant.id) {
      let variants = product.variants

      if (!Array.isArray(variants) || variants.length === 0) {
        variants = await cartService.getProductVariants(product.id)
      }

      if (!variants?.length) {
        throw new Error('Sản phẩm chưa có SKU hoặc đã hết hàng.')
      }

      variant =
        variants.find(
          (item) =>
            item?.id &&
            (item.stockQty == null || Number(item.stockQty) > 0)
        ) || variants[0]
    }

    if (!variant?.id) {
      throw new Error('Không tìm thấy SKU hợp lệ cho sản phẩm.')
    }

    if (variant.stockQty != null && Number(variant.stockQty) < Number(quantity)) {
      throw new Error(`Sản phẩm chỉ còn ${variant.stockQty} sản phẩm trong kho.`)
    }

    const data = await cartService.addCartItem({
      productId: Number(product.id),
      variantId: Number(variant.id),
      size: variant.size || product.sizes?.[0] || 'M',
      quantity: Number(quantity) || 1,
    })

    setCart(data)

    return data
  }, [isCustomer])

  // =============================
  // XÓA SẢN PHẨM
  // =============================
  const removeItem = useCallback(async function removeItem(id) {

    try {

      const data =
        await cartService.removeCartItem(id)

      setCart(data)

      return data

    } catch (error) {

      console.error(
        'Lỗi xóa sản phẩm:',
        error
      )

      throw error
    }
  }, [])

  // =============================
  // CẬP NHẬT SỐ LƯỢNG
  // =============================
  const updateQuantity = useCallback(async function updateQuantity(item, quantity) {

    const newQuantity = Number(quantity)

    if (newQuantity < 1) {
      return removeItem(item.id)
    }

    try {

      const data =
        await cartService.updateCartItem(
          item.id,
          {
            productId: item.productId,
            variantId: item.variantId,
            size: item.size,
            quantity: newQuantity,
          }
        )

      setCart(data)

      return data

    } catch (error) {

      console.error(
        'Lỗi cập nhật số lượng:',
        error
      )

      throw error
    }
  }, [removeItem])

  // =============================
  // TĂNG
  // =============================
  const increaseQuantity = useCallback(async function increaseQuantity(item) {

    return updateQuantity(
      item,
      Number(item.quantity) + 1
    )
  }, [updateQuantity])

  // =============================
  // GIẢM
  // =============================
  const decreaseQuantity = useCallback(async function decreaseQuantity(item) {

    const quantity =
      Number(item.quantity) - 1

    if (quantity <= 0) {
      return removeItem(item.id)
    }

    return updateQuantity(
      item,
      quantity
    )
  }, [removeItem, updateQuantity])

  const visibleCart =
    isCustomer
      ? cart
      : null

  const items = useMemo(() => (
    Array.isArray(visibleCart?.items)
      ? visibleCart.items
      : []
  ), [visibleCart])

  const total = useMemo(() => (
    items.reduce(
      (sum, item) => {

        const price =
          Number(item.price || 0)

        const quantity =
          Number(item.quantity || 0)

        return (
          sum +
          price * quantity
        )
      },
      0
    )
  ), [items])

  const itemCount = useMemo(() => (
    items.reduce(
      (sum, item) =>
        sum + Number(item.quantity || 0),
      0
    )
  ), [items])

  const value = useMemo(() => ({

    cart:
      visibleCart,

    items,

    total,

    itemCount,

    cartLoading,

    setCart,

    loadCart,

    addToCart,

    updateQuantity,

    increaseQuantity,

    decreaseQuantity,

    removeItem,
  }), [
    visibleCart,
    items,
    total,
    itemCount,
    cartLoading,
    loadCart,
    addToCart,
    updateQuantity,
    increaseQuantity,
    decreaseQuantity,
    removeItem,
  ])

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  )
}
