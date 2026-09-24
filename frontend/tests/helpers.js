import { expect } from '@playwright/test'

export function uniqueEmail(prefix = 'e2e') {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10000)}@example.com`
}

export async function registerCustomer(page, email = uniqueEmail()) {
  await page.goto('/#register')
  await page.getByLabel('Họ và tên').fill('Playwright Customer')
  await page.getByLabel('Email').fill(email)
  await page.locator('form input[type="password"]').fill('E2eTest@12345')
  await page.getByLabel('Số điện thoại').fill('0900000011')
  await page.getByLabel('Địa chỉ').fill('E2E test address')
  await page.locator('form').getByRole('button', { name: 'Đăng ký', exact: true }).click()
  await expect(page).toHaveURL(/#login$/)
  return { email, password: 'E2eTest@12345' }
}

export async function loginCustomer(page) {
  const account = await registerCustomer(page)
  await page.getByLabel('Email').fill(account.email)
  await page.locator('form input[type="password"]').fill(account.password)
  await page.locator('form').getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await expect(page).toHaveURL(/#home$/)
  return account
}

export async function openProducts(page) {
  await page.goto('/#products')
  await expect(page.getByRole('heading', { name: /Sản phẩm/i }).first()).toBeVisible()
  await expect(page.locator('.product-card').first()).toBeVisible()
}

export async function openFirstProduct(page) {
  await openProducts(page)
  await page.locator('.product-card').first().getByRole('button', { name: /Xem chi tiết/i }).click()
  await expect(page.locator('.product-detail-experience')).toBeVisible()
}

export async function addFirstProductToCart(page) {
  await openFirstProduct(page)
  await page.locator('.detail-add-button').click()
  await page.goto('/#cart')
  await expect(page.locator('.cart-item-card').first()).toBeVisible()
}
