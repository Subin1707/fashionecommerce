import { test, expect } from '@playwright/test'

const firstEvent = {
  id: 11,
  eventName: 'Summer Sale',
  title: 'HĂ¨ rá»±c rá»¡',
  message: 'LĂ m má»›i phong cĂ¡ch mĂ¹a hĂ¨.',
  desktopImageUrl: 'https://example.com/summer-desktop.jpg',
  mobileImageUrl: 'https://example.com/summer-mobile.jpg',
  startAt: '2026-10-19T00:00:00',
  endAt: '2026-10-30T23:59:00',
  offerLabel: 'Giáº£m 10%',
  voucherCode: 'SUMMER10',
  targetUrl: 'products',
  status: 'ACTIVE',
  priority: 20,
}

const secondEvent = {
  ...firstEvent,
  id: 12,
  eventName: 'New Collection',
  title: 'NĂ©t riĂªng mĂ¹a má»›i',
  message: 'Nhá»¯ng thiáº¿t káº¿ vá»«a ra máº¯t.',
  offerLabel: '',
  voucherCode: null,
  priority: 10,
}

async function setAuth(page, role) {
  await page.addInitScript((authRole) => {
    localStorage.setItem('fashion.auth', JSON.stringify({ token: 'e2e-token', role: authRole, email: 'e2e@example.com' }))
  }, role)
}

test('campaign carousel navigates, advances, and stays usable on mobile', async ({ page }) => {
  await setAuth(page, 'CUSTOMER')
  await page.route('**/api/events/active', (route) => route.fulfill({ json: [firstEvent, secondEvent] }))
  await page.route('**/api/cart', (route) => route.fulfill({ json: { items: [] } }))
  await page.route('**/api/brands', (route) => route.fulfill({ json: [] }))

  await page.goto('/#home')
  const banner = page.locator('.campaign-banner')
  await expect(banner).toBeVisible()
  await expect(banner).toContainText('HĂ¨ rá»±c rá»¡')
  await page.getByRole('button', { name: 'Tạo sự kiện' }).click()
  await expect(banner).toContainText('NĂ©t riĂªng mĂ¹a má»›i')
  await page.waitForTimeout(5100)
  await expect(banner).toContainText('HĂ¨ rá»±c rá»¡')

  await page.setViewportSize({ width: 390, height: 844 })
  const bounds = await banner.boundingBox()
  expect(bounds.width).toBeLessThanOrEqual(390)
  expect(bounds.height).toBeGreaterThan(0)
  await expect(banner).toContainText('HĂ¨ rá»±c rá»¡')
})

test('admin can create, edit, publish, hide, and delete a campaign', async ({ page }) => {
  await setAuth(page, 'ADMIN')
  let events = []
  let nextId = 20

  await page.route('**/api/admin/events', async (route) => {
    if (route.request().method() === 'GET') return route.fulfill({ json: events })
    if (route.request().method() === 'POST') {
      const created = { ...route.request().postDataJSON(), id: nextId++ }
      events = [created]
      return route.fulfill({ json: created })
    }
    return route.continue()
  })
  await page.route('**/api/admin/events/*', async (route) => {
    const pathname = new URL(route.request().url()).pathname
    const method = route.request().method()
    if (pathname.endsWith('/images')) {
      return route.fulfill({ json: { imageUrl: '/uploads/events/11111111-1111-4111-8111-111111111111.png' } })
    }
    const id = Number(pathname.split('/').at(-1))
    if (method === 'PUT') {
      const updated = { ...events.find((item) => item.id === id), ...route.request().postDataJSON(), id }
      events = events.map((item) => item.id === id ? updated : item)
      return route.fulfill({ json: updated })
    }
    if (method === 'DELETE') {
      events = events.filter((item) => item.id !== id)
      return route.fulfill({ status: 204 })
    }
    return route.continue()
  })

  await page.goto('/#admin-events')
  await page.getByRole('button', { name: 'Sự kiện', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Banner sự kiện' })).toBeVisible()
  await page.getByLabel('Tên sự kiện').fill('Black Friday')
  await page.getByLabel('Tiêu đề trên banner').fill('Ưu đãi cuối năm')
  await page.locator('input[type="file"]').first().setInputFiles({
    name: 'banner.png',
    mimeType: 'image/png',
    buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p1sAAAAASUVORK5CYII=', 'base64'),
  })
  await expect(page.getByRole('status').filter({ hasText: 'Đã tải ảnh banner lên.' })).toBeVisible()
  await page.getByRole('button', { name: 'Tạo sự kiện' }).click()
  await expect(page.getByRole('heading', { name: 'Black Friday' })).toBeVisible()
  await expect(page.getByRole('status').filter({ hasText: 'Banner đã xuất bản và đang hiển thị trên trang chủ.' })).toBeVisible()

  await page.getByRole('button', { name: 'Sửa' }).click()
  await page.getByLabel('Tiêu đề trên banner').fill('Black Friday — ưu đãi mới')
  await page.getByRole('button', { name: 'Lưu sự kiện' }).click()
  await expect(page.getByText('Black Friday — ưu đãi mới')).toBeVisible()

  await page.getByRole('button', { name: 'Ẩn' }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Đã ẩn sự kiện.' })).toBeVisible()
  page.on('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: 'Xóa' }).click()
  await expect(page.getByText('Chưa có sự kiện. Tạo chiến dịch đầu tiên để xuất hiện trên trang chủ.')).toBeVisible()
})


