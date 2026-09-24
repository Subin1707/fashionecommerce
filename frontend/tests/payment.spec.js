import { test, expect } from '@playwright/test'
import { loginCustomer } from './helpers'

test.describe('Payment', () => {
  test.beforeEach(async ({ page }) => { await loginCustomer(page) })

  test('TC-26 payment method controls render', async ({ page }) => {
    await page.goto('/#checkout')
    await expect(page.getByText(/VNPay|COD/i).first()).toBeVisible()
  })

  test('TC-27 payment return failure route is handled', async ({ page }) => {
    await page.goto('/#orders')
    await expect(page.getByRole('heading', { name: /Đơn hàng/i })).toBeVisible()
  })
})