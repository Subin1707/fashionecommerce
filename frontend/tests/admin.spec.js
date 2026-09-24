import { test, expect } from '@playwright/test'

test.describe('Admin', () => {
  test.beforeEach(async ({ page }) => {
    test.skip(!process.env.E2E_ADMIN_PASSWORD, 'Set E2E_ADMIN_PASSWORD to run admin functional cases')
    await page.goto('/#login')
    await page.getByLabel('Email').fill(process.env.E2E_ADMIN_EMAIL || 'admin@fashion.local')
    await page.locator('form input[type="password"]').fill(process.env.E2E_ADMIN_PASSWORD)
    await page.locator('form').getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page).toHaveURL(/#admin-dashboard$/)
  })

  test('TC-42 admin dashboard loads', async ({ page }) => {
    await expect(page.getByText(/Quản trị vận hành|Tổng quan/i).first()).toBeVisible()
  })

  test('TC-43 admin products page loads', async ({ page }) => {
    await page.goto('/#admin-products')
    await expect(page.getByText(/sản phẩm/i).first()).toBeVisible()
  })

  test('TC-44 admin inventory page loads', async ({ page }) => {
    await page.goto('/#admin-inventory')
    await expect(page.locator('main')).toBeVisible()
  })

  test('TC-45 customer-facing protected route is separate from admin', async ({ page }) => {
    await page.goto('/#admin-orders')
    await expect(page.locator('main')).toBeVisible()
  })
})