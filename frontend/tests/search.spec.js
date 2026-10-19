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

  test('category selection sends categoryId and displays filtered products', async ({ page }) => {
    await page.route('**/api/categories', (route) => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{ id: 42, name: 'Quần', slug: 'quan', isActive: true }]),
    }))
    await page.route('**/api/search?**', async (route) => {
      const url = new URL(route.request().url())
      const filtered = url.searchParams.get('categoryId') === '42'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: filtered ? [{
            id: 901,
            name: 'Quần test',
            slug: 'quan-test',
            categoryName: 'Quần',
            basePrice: 100000,
            salePrice: 100000,
            totalStock: 1,
            colors: [],
            sizes: [],
          }] : [],
          totalPages: 1,
        }),
      })
    })

    await page.goto('/#products')
    const category = page.getByRole('combobox', { name: 'Danh mục sản phẩm' })
    await expect(category).toBeVisible()
    await category.selectOption('42')
    await page.getByRole('button', { name: 'Tìm kiếm' }).click()
    await expect(page.getByRole('heading', { name: 'Quần test' })).toBeVisible()
  })
})
