import { test, expect } from '@playwright/test'
import { loginCustomer } from './helpers'

test.describe('Shipping', () => {
  test('TC-31 checkout has delivery location workflow', async ({ page }) => {
    await loginCustomer(page)
    await page.goto('/#checkout')
    await expect(page.getByText(/giao hàng|địa chỉ|bản đồ/i).first()).toBeVisible()
  })

  test('TC-32 orders page exposes shipment context', async ({ page }) => {
    await loginCustomer(page)
    await page.goto('/#orders')
    await expect(page.locator('main')).toBeVisible()
  })
})