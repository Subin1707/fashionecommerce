import { test, expect } from '@playwright/test'
import { loginCustomer, openFirstProduct } from './helpers'

test.describe('Wishlist', () => {
  test.beforeEach(async ({ page }) => { await loginCustomer(page) })

  test('TC-36 wishlist page loads', async ({ page }) => {
    await page.goto('/#wishlist')
    await expect(page.getByRole('heading', { name: /yêu thích/i })).toBeVisible()
  })

  test('TC-37 product can be added to wishlist', async ({ page }) => {
    await openFirstProduct(page)
    const toggle = page.locator('.detail-wishlist-toggle')
    await toggle.click()
    await expect(toggle).toHaveAttribute('aria-pressed', 'true')
  })

  test('TC-38 wishlist toggle removes product', async ({ page }) => {
    await openFirstProduct(page)
    const toggle = page.locator('.detail-wishlist-toggle')
    if (await toggle.getAttribute('aria-pressed') !== 'true') {
      await toggle.click()
      await expect(toggle).toHaveAttribute('aria-pressed', 'true')
    }
    await toggle.click()
    await expect(toggle).toHaveAttribute('aria-pressed', 'false')
  })
})
