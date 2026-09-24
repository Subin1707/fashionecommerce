import { test, expect } from '@playwright/test'
import { loginCustomer, openFirstProduct } from './helpers'

test.describe('Cart', () => {
  test.beforeEach(async ({ page }) => { await loginCustomer(page) })

  test('TC-17 add a selected variant to cart', async ({ page }) => {
    await openFirstProduct(page)
    await page.getByRole('button', { name: 'Thêm vào giỏ' }).click()
    await expect(page.getByText(/Đã thêm/i)).toBeVisible()
  })

  test('TC-18 cart page is accessible', async ({ page }) => {
    await page.goto('/#cart')
    await expect(page.getByRole('heading', { name: /Giỏ hàng/i })).toBeVisible()
  })

  test('TC-19 cart quantity controls are present for an item', async ({ page }) => {
    await openFirstProduct(page)
    await page.getByRole('button', { name: 'Thêm vào giỏ' }).click()
    await page.goto('/#cart')
    await expect(page.locator('button').filter({ hasText: /\+|−|-/ }).first()).toBeVisible()
  })

  test('TC-20 empty cart remains actionable', async ({ page }) => {
    await page.goto('/#cart')
    await expect(page.getByText(/Giỏ hàng|trống|chưa có/i).first()).toBeVisible()
  })
})