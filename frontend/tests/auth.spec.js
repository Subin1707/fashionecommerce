import { test, expect } from '@playwright/test'
import { registerCustomer } from './helpers'

test.describe('Authentication', () => {
  test('TC-01 register succeeds', async ({ page }) => {
    await registerCustomer(page)
    await expect(page).toHaveURL(/#login$/)
  })

  test('TC-02 duplicate email is rejected', async ({ page }) => {
    const account = await registerCustomer(page)
    await page.goto('/#register')
    await page.getByLabel('Họ và tên').fill('Duplicate')
    await page.getByLabel('Email').fill(account.email)
    await page.locator('form input[type="password"]').fill(account.password)
    await page.getByLabel('Số điện thoại').fill('0900000012')
    await page.getByLabel('Địa chỉ').fill('Duplicate address')
    const responsePromise = page.waitForResponse((response) => (
      response.url().includes('/api/auth/register') && response.request().method() === 'POST'
    ))
    await page.locator('form').getByRole('button', { name: 'Đăng ký', exact: true }).click()
    const response = await responsePromise
    expect(response.status()).toBeGreaterThanOrEqual(400)
    await expect(page).toHaveURL(/#register$/)
  })

  test('TC-03 login succeeds', async ({ page }) => {
    const account = await registerCustomer(page)
    await page.getByLabel('Email').fill(account.email)
    await page.locator('form input[type="password"]').fill(account.password)
    await page.locator('form').getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page).toHaveURL(/#home$/)
  })

  test('TC-04 wrong password is rejected', async ({ page }) => {
    const account = await registerCustomer(page)
    await page.getByLabel('Email').fill(account.email)
    await page.locator('form input[type="password"]').fill('wrong-password')
    const responsePromise = page.waitForResponse((response) => (
      response.url().includes('/api/auth/login') && response.request().method() === 'POST'
    ))
    await page.locator('form').getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    const response = await responsePromise
    expect(response.status()).toBeGreaterThanOrEqual(400)
    await expect(page).toHaveURL(/#login$/)
  })

  test('TC-05 invalid register fields show validation', async ({ page }) => {
    await page.goto('/#register')
    await page.locator('form').getByRole('button', { name: 'Đăng ký', exact: true }).click()
    await expect(page.locator('.field-error')).toHaveCount(5)
  })
})
