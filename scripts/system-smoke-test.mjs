const baseUrl = process.env.SMOKE_BASE_URL || 'http://127.0.0.1:5173'
const stamp = Date.now()

const state = {
  adminToken: null,
  customerToken: null,
  customerEmail: null,
  brandId: null,
  categoryId: null,
  productId: null,
  variantId: null,
  cartItemId: null,
  orderId: null,
  reviewId: null,
  voucherCode: `SMOKE${stamp}`,
}

async function main() {
  await step('Frontend app loads', async () => {
    const response = await fetch(`${baseUrl}/`)
    assert(response.ok, `Expected frontend 200, got ${response.status}`)
    const html = await response.text()
    assert(html.includes('<div id="root">'), 'Expected React root element')
  })

  await step('Public product/search/category/brand endpoints load', async () => {
    await api('GET', '/api/products')
    await api('GET', '/api/search')
    await api('GET', '/api/categories')
    await api('GET', '/api/brands')
  })

  await step('Admin login', async () => {
    const auth = await api('POST', '/api/auth/login', {
      email: 'admin@fashion.local',
      password: 'Admin@123',
    })
    assert(auth.token, 'Missing admin token')
    assert(auth.role === 'ADMIN', `Expected ADMIN role, got ${auth.role}`)
    state.adminToken = auth.token
  })

  await step('Admin creates brand, category, product, variant, voucher', async () => {
    const brand = await api('POST', '/api/brands', {
      name: `Smoke Brand ${stamp}`,
      slug: `smoke-brand-${stamp}`,
      description: 'Smoke test brand',
      isActive: true,
    }, state.adminToken)
    state.brandId = brand.id

    const category = await api('POST', '/api/categories', {
      name: `Smoke Category ${stamp}`,
      slug: `smoke-category-${stamp}`,
      description: 'Smoke test category',
      isActive: true,
    }, state.adminToken)
    state.categoryId = category.id

    const product = await api('POST', '/api/admin/products', {
      brandId: state.brandId,
      categoryId: state.categoryId,
      name: `Smoke Black Dress ${stamp}`,
      description: 'Black dress created by smoke test',
      basePrice: 500000,
      salePrice: 450000,
      material: 'Cotton',
      fit: 'Regular',
      gender: 'FEMALE',
      status: 'ACTIVE',
      isFeatured: true,
      isNew: true,
    }, state.adminToken)
    state.productId = product.id

    const variant = await api('POST', `/api/admin/products/${state.productId}/variants`, {
      color: 'Black',
      size: 'M',
      stockQty: 10,
      priceAdjustment: 0,
    }, state.adminToken)
    state.variantId = variant.id

    const startDate = new Date(Date.now() - 60_000).toISOString().slice(0, 19)
    const endDate = new Date(Date.now() + 86_400_000).toISOString().slice(0, 19)
    await api('POST', '/api/vouchers', {
      code: state.voucherCode,
      discountType: 'PERCENTAGE',
      discountValue: 10,
      minOrderValue: 100000,
      maxDiscount: 100000,
      startDate,
      endDate,
      status: 'ACTIVE',
    }, state.adminToken)
  })

  await step('Customer register/login', async () => {
    const email = `smoke-${stamp}@example.com`
    await api('POST', '/api/auth/register', {
      fullName: 'Smoke Customer',
      email,
      password: 'Customer@123',
      phone: '0900000002',
      address: 'Smoke Address',
    })
    const auth = await api('POST', '/api/auth/login', { email, password: 'Customer@123' })
    assert(auth.token, 'Missing customer token')
    assert(auth.role === 'CUSTOMER', `Expected CUSTOMER role, got ${auth.role}`)
    state.customerToken = auth.token
    state.customerEmail = email
  })

  await step('Public product detail and search fallback', async () => {
    const product = await api('GET', `/api/products/${state.productId}`)
    assert(product.id === state.productId, 'Product detail returned wrong product')
    const search = await api('GET', `/api/search?keyword=${encodeURIComponent('Black Dress')}`)
    const rows = Array.isArray(search) ? search : search.content
    assert(rows.some((item) => item.id === state.productId), 'Search did not include created product')
  })

  await step('Cart add/read/update', async () => {
    const cart = await api('POST', '/api/cart/items', {
      productId: state.productId,
      variantId: state.variantId,
      size: 'M',
      quantity: 2,
    }, state.customerToken)
    assert(cart.items?.length, 'Cart did not contain added item')
    state.cartItemId = cart.items[0].id
    await api('GET', '/api/cart', null, state.customerToken)
    const updated = await api('PUT', `/api/cart/items/${state.cartItemId}`, {
      quantity: 1,
    }, state.customerToken)
    assert(updated.items?.[0]?.quantity === 1, 'Cart item quantity was not updated')
  })

  await step('Voucher validation and checkout summary', async () => {
    const voucher = await api('GET', `/api/vouchers/validate?code=${state.voucherCode}&orderValue=450000`, null, state.customerToken)
    assert(voucher.valid === true, 'Voucher should be valid')
    const summary = await api('GET', '/api/checkout/summary', null, state.customerToken)
    assert(summary.finalAmount > 0, 'Checkout summary finalAmount should be positive')
  })

  await step('Checkout, customer orders, payment, admin order lifecycle', async () => {
    const order = await api('POST', '/api/checkout', {
      shippingAddress: '123 Smoke Street',
      paymentMethod: 'COD',
      voucherCode: state.voucherCode,
    }, state.customerToken)
    state.orderId = order.id
    assert(order.orderStatus === 'PENDING', `Expected PENDING, got ${order.orderStatus}`)
    await api('GET', '/api/orders', null, state.customerToken)
    await api('GET', `/api/orders/${state.orderId}`, null, state.customerToken)
    await api('PATCH', `/api/orders/${state.orderId}/payment`, null, state.customerToken)

    for (const status of ['CONFIRMED', 'PROCESSING']) {
      await api('PUT', `/api/admin/orders/${state.orderId}/status`, { orderStatus: status }, state.adminToken)
    }
    const providers = await api('GET', '/api/admin/shipping-providers', null, state.adminToken)
    assert(providers.length > 0, 'Missing shipping providers')
    await api('POST', `/api/admin/orders/${state.orderId}/shipment`, {
      providerId: providers[0].id, weightKg: 1.2, shippingFee: 30000,
    }, state.adminToken)
    for (const status of ['PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED']) {
      await api('PATCH', `/api/admin/orders/${state.orderId}/shipment/simulate`, { status }, state.adminToken)
    }
    const tracking = await api('GET', `/api/orders/${state.orderId}/shipment`, null, state.customerToken)
    assert(tracking.shipment.status === 'DELIVERED', 'Shipment was not delivered')
    const received = await api('PATCH', `/api/orders/${state.orderId}/receive`, null, state.customerToken)
    state.orderItemId = received.items[0].id
    const adminOrder = await api('GET', `/api/admin/orders/${state.orderId}`, null, state.adminToken)
    assert(adminOrder.orderStatus === 'COMPLETED', `Expected COMPLETED, got ${adminOrder.orderStatus}`)
  })

  await step('Review create/list/moderate', async () => {
    const review = await api('POST', `/api/reviews/product/${state.productId}`, {
      rating: 5,
      orderItemId: state.orderItemId,
      comment: 'Smoke test review',
    }, state.customerToken)
    state.reviewId = review.id
    const reviews = await api('GET', `/api/reviews/product/${state.productId}`, null, state.customerToken)
    assert(reviews.some((item) => item.id === state.reviewId), 'Review list missing created review')
    const hidden = await api('PUT', `/api/reviews/${state.reviewId}/status?status=HIDDEN`, null, state.adminToken)
    assert(hidden.status === 'HIDDEN', `Expected HIDDEN, got ${hidden.status}`)
  })

  await step('Wishlist add/list/remove', async () => {
    await api('POST', `/api/wishlist/${state.productId}`, null, state.customerToken)
    const items = await api('GET', '/api/wishlist', null, state.customerToken)
    assert(items.some((item) => item.productId === state.productId), 'Wishlist missing created product')
    await api('DELETE', `/api/wishlist/${state.productId}`, null, state.customerToken)
  })

  await step('Chatbot and smart size', async () => {
    const chat = await api('POST', '/api/chatbot', {
      message: 'I want a black dress',
    }, state.customerToken)
    assert(typeof chat === 'string' && chat.length > 0, 'Chatbot returned empty response')
    const size = await api('POST', `/api/smart-size/products/${state.productId}`, {
      height: 165,
      weight: 55,
      chest: 86,
      waist: 70,
      hip: 92,
      age: 25,
    }, state.customerToken)
    assert(size.recommendedSize, 'Smart size missing recommendedSize')
  })

  await step('Admin dashboard/customers/products/orders', async () => {
    await api('GET', '/api/admin/dashboard', null, state.adminToken)
    await api('GET', '/api/users', null, state.adminToken)
    const customers = await api('GET', '/api/admin/customers', null, state.adminToken)
    assert(customers.some((item) => item.email === state.customerEmail), 'Admin customer list missing customer')
    await api('GET', '/api/admin/products?page=0&size=8', null, state.adminToken)
    await api('GET', '/api/admin/orders?page=0&size=8', null, state.adminToken)
  })
}

async function step(name, fn) {
  try {
    await fn()
    console.log(`PASS ${name}`)
  } catch (error) {
    console.error(`FAIL ${name}`)
    console.error(error.message)
    process.exitCode = 1
    throw error
  }
}

async function api(method, path, body, token) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body == null ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body == null ? undefined : JSON.stringify(body),
  })
  const text = await response.text()
  const data = parseBody(text)
  if (!response.ok) {
    throw new Error(`${method} ${path} -> HTTP ${response.status}: ${text}`)
  }
  return data
}

function parseBody(text) {
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

main().catch(() => process.exit(1))
