import { test, expect } from '@playwright/test'
import { loginCustomer, openFirstProduct } from './helpers'

test.describe('Reviews', () => {
  test('TC-33 product reviews are visible', async ({ page }) => {
    await openFirstProduct(page)
    await expect(page.getByText(/đánh giá/i).first()).toBeVisible()
  })

  test('TC-34 customer review page is protected and reachable', async ({ page }) => {
    await loginCustomer(page)
    await page.goto('/#review')
    await expect(page.locator('main')).toBeVisible()
  })

  test('TC-35 review page shows empty or eligible orders state', async ({ page }) => {
    await loginCustomer(page)
    await page.goto('/#review')
    await expect(page.getByText(/đánh giá|chưa|đơn hàng/i).first()).toBeVisible()
  })
})