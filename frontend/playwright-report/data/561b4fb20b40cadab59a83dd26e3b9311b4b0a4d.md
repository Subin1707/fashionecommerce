# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: cart.spec.js >> Cart >> TC-17 add a selected variant to cart
- Location: tests\cart.spec.js:7:3

# Error details

```
Test timeout of 30000ms exceeded while running "beforeEach" hook.
```

```
Tearing down "context" exceeded the test timeout of 30000ms.
```

```
Error: page.goto: Test timeout of 30000ms exceeded.
Call log:
  - navigating to "http://localhost:5173/#register", waiting until "load"

```

# Test source

```ts
  1  | import { expect } from '@playwright/test'
  2  | 
  3  | export function uniqueEmail(prefix = 'e2e') {
  4  |   return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10000)}@example.com`
  5  | }
  6  | 
  7  | export async function registerCustomer(page, email = uniqueEmail()) {
> 8  |   await page.goto('/#register')
     |              ^ Error: page.goto: Test timeout of 30000ms exceeded.
  9  |   await page.getByLabel('Họ và tên').fill('Playwright Customer')
  10 |   await page.getByLabel('Email').fill(email)
  11 |   await page.locator('form input[type="password"]').fill('E2eTest@12345')
  12 |   await page.getByLabel('Số điện thoại').fill('0900000011')
  13 |   await page.getByLabel('Địa chỉ').fill('E2E test address')
  14 |   await page.locator('form').getByRole('button', { name: 'Đăng ký', exact: true }).click()
  15 |   await expect(page).toHaveURL(/#login$/)
  16 |   return { email, password: 'E2eTest@12345' }
  17 | }
  18 | 
  19 | export async function loginCustomer(page) {
  20 |   const account = await registerCustomer(page)
  21 |   await page.getByLabel('Email').fill(account.email)
  22 |   await page.locator('form input[type="password"]').fill(account.password)
  23 |   await page.locator('form').getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  24 |   await expect(page).toHaveURL(/#home$/)
  25 |   return account
  26 | }
  27 | 
  28 | export async function openProducts(page) {
  29 |   await page.goto('/#products')
  30 |   await expect(page.getByRole('heading', { name: /Sản phẩm/i }).first()).toBeVisible()
  31 |   await expect(page.locator('.product-card').first()).toBeVisible()
  32 | }
  33 | 
  34 | export async function openFirstProduct(page) {
  35 |   await openProducts(page)
  36 |   await page.locator('.product-card').first().getByRole('button', { name: /Xem chi tiết/i }).click()
  37 |   await expect(page.locator('.product-detail-experience')).toBeVisible()
  38 | }
  39 | 
  40 | export async function addFirstProductToCart(page) {
  41 |   await openFirstProduct(page)
  42 |   await page.locator('.detail-add-button').click()
  43 |   await page.goto('/#cart')
  44 |   await expect(page.locator('.cart-item-card').first()).toBeVisible()
  45 | }
  46 | 
```