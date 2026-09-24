import { test, expect } from '@playwright/test'
import { loginCustomer } from './helpers'

test.describe('Checkout', () => {
  test.beforeEach(async ({ page }) => { await loginCustomer(page); await page.goto('/#checkout') })

  test('TC-23 checkout form renders', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /Thanh toán|đặt hàng/i })).toBeVisible()
  })

  test('TC-24 required checkout data is validated', async ({ page }) => {
    const submit = page.getByRole('button', { name: /Đặt hàng|Thanh toán/i }).last()
    await submit.click()
    await expect(page.getByText(/Vui lòng|địa chỉ/i).last()).toBeVisible()
  })

  test('TC-25 COD payment option is available', async ({ page }) => {
    await expect(page.getByText(/COD|Thanh toán khi nhận/i).first()).toBeVisible()
  })
})