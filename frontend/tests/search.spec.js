import { test, expect } from '@playwright/test'
import { openProducts } from './helpers'

test.describe('Search and filtering', () => {
  test('TC-14 category filter controls render', async ({ page }) => {
    await openProducts(page)
    await expect(page.locator('.product-toolbar select').first()).toBeVisible()
    await expect(page.locator('.product-toolbar input').first()).toBeVisible()
  })

  test('TC-15 sort control is usable', async ({ page }) => {
    await openProducts(page)
    const sort = page.locator('.product-toolbar select').last()
    await sort.selectOption('featured')
    await expect(sort).toHaveValue('featured')
  })

  test('TC-16 reset filters is available', async ({ page }) => {
    await openProducts(page)
    await expect(page.locator('.product-toolbar button.ghost')).toBeVisible()
  })
})
