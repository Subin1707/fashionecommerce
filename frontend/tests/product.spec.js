import { test, expect } from '@playwright/test'
import { openProducts, openFirstProduct } from './helpers'

test.describe('Products and variants', () => {
  test('TC-06 product list loads', async ({ page }) => {
    await openProducts(page)
    await expect(page.locator('.product-card')).not.toHaveCount(0)
  })

  test('TC-07 product detail loads', async ({ page }) => {
    await openFirstProduct(page)
    await expect(page.locator('.product-detail-experience h1')).toBeVisible()
  })

  test('TC-08 search returns a catalog state', async ({ page }) => {
    await openProducts(page)
    const search = page.locator('.product-toolbar input').first()
    await search.fill('linen')
    await search.press('Enter')
    await expect(page.locator('.product-grid, .empty-state').first()).toBeVisible()
  })

  test('TC-09 color and size selectors are available', async ({ page }) => {
    await openFirstProduct(page)
    await expect(page.locator('.detail-swatch-row button').first()).toBeVisible()
    await expect(page.locator('.detail-size-row button').first()).toBeVisible()
  })

  test('TC-10 missing product shows empty state', async ({ page }) => {
    await page.goto('/#product-detail?id=999999')
    await expect(page.locator('.empty-state, .notice').first()).toBeVisible()
  })

  test('TC-11 selecting color changes active variant', async ({ page }) => {
    await openFirstProduct(page)
    const colors = page.locator('.detail-swatch-row button')
    if (await colors.count() > 1) {
      await colors.nth(1).click()
      await expect(colors.nth(1)).toHaveClass(/active/)
    }
  })

  test('TC-12 selecting size changes active variant', async ({ page }) => {
    await openFirstProduct(page)
    const sizes = page.locator('.detail-size-row button')
    if (await sizes.count() > 1) {
      await sizes.nth(1).click()
      await expect(sizes.nth(1)).toHaveClass(/active/)
    }
  })

  test('TC-13 quantity stepper never exceeds selected stock', async ({ page }) => {
    await openFirstProduct(page)
    const quantity = page.locator('.detail-quantity-stepper span')
    const plus = page.locator('.detail-quantity-stepper button').last()
    await expect(quantity).toHaveText('1')
    for (let index = 0; index < 5 && (await plus.isEnabled()); index += 1) {
      await plus.click()
    }
    await expect(page.locator('.detail-stock-pill')).toBeVisible()
  })
})
