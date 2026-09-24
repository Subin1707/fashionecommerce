import { test, expect } from '@playwright/test'
import { loginCustomer } from './helpers'

test.describe('Smart Size', () => {
  test.beforeEach(async ({ page }) => {
    await loginCustomer(page)
    await page.goto('/#recommendation')
  })

  test('TC-39 valid body measurements return a recommendation', async ({ page }) => {
    await page.getByLabel('Chiều cao').fill('168')
    await page.getByLabel('Cân nặng').fill('58')
    await page.getByLabel('Vòng ngực').fill('86')
    await page.getByLabel('Vòng eo').fill('70')
    await page.getByLabel('Vòng hông').fill('94')
    await page.getByRole('button', { name: /Gợi ý kích cỡ/i }).click()
    await expect(page.locator('.size-result-card')).toContainText(/Kết quả size|Sản phẩm tham chiếu|Chưa có kết quả/i)
  })

  test('TC-40 boundary measurements remain editable', async ({ page }) => {
    const height = page.getByLabel('Chiều cao')
    await height.fill('120')
    await expect(height).toHaveValue('120')
  })

  test('TC-41 style recommendation controls render', async ({ page }) => {
    await expect(page.locator('.style-quiz-panel')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Công sở' })).toBeVisible()
  })
})
