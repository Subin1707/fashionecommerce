import { test, expect } from '@playwright/test'
import { addFirstProductToCart, loginCustomer } from './helpers'

test.describe('Voucher', () => {
  test.beforeEach(async ({ page }) => {
    await loginCustomer(page)
    await addFirstProductToCart(page)
  })

  test('TC-21 voucher input is available', async ({ page }) => {
    await expect(page.locator('.summary-voucher-section input')).toBeVisible()
  })

  test('TC-22 invalid voucher is rejected in UI', async ({ page }) => {
    await page.locator('.summary-voucher-section input').fill('INVALID-CODE')
    await page.locator('.voucher-apply-btn').click()
    await expect(page.locator('.summary-voucher-section')).toContainText(/không hợp lệ|hết hạn/i)
  })
})
