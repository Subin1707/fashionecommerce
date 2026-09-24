import { test, expect } from '@playwright/test'
import { loginCustomer } from './helpers'

test.describe('Orders', () => {
  test.beforeEach(async ({ page }) => { await loginCustomer(page) })

  test('TC-28 order list loads', async ({ page }) => {
    await page.goto('/#orders')
    await expect(page.getByRole('heading', { name: /Đơn hàng/i })).toBeVisible()
  })

  test('TC-29 profile page loads', async ({ page }) => {
    await page.goto('/#profile')
    await expect(page.getByRole('heading', { name: /hồ sơ|tài khoản/i })).toBeVisible()
  })

  test('TC-30 order detail route handles missing id', async ({ page }) => {
    await page.goto('/#order-detail?id=999999')
    await expect(page.locator('main')).toBeVisible()
  })
})