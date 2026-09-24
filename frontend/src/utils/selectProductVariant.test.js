import { test } from 'node:test'
import assert from 'node:assert/strict'
import { selectProductVariant } from './selectProductVariant.js'

const whiteM = { id: 1, color: 'Trang', size: 'M', stockQty: 13 }
const brownL = { id: 2, color: 'Nau', size: 'L', stockQty: 5 }
const variants = [whiteM, brownL]

test('brown then L matches only the requested combination', () => {
  const selection = { color: 'Nau', size: 'M' }
  assert.equal(selectProductVariant(variants, selection), undefined)
  assert.equal(selection.size, 'M')
  selection.size = 'L'
  assert.equal(selectProductVariant(variants, selection), brownL)
})

test('L then brown matches the same combination', () => {
  const selection = { color: 'Trang', size: 'L' }
  assert.equal(selectProductVariant(variants, selection), undefined)
  selection.color = 'Nau'
  assert.equal(selectProductVariant(variants, selection), brownL)
})

test('missing combinations do not fall back to another variant', () => {
  assert.equal(selectProductVariant(variants, { color: 'Nau', size: 'S' }), undefined)
})

test('sold-out combinations remain selected', () => {
  const brownM = { ...brownL, id: 3, size: 'M', stockQty: 0 }
  assert.equal(selectProductVariant([...variants, brownM], { color: 'Nau', size: 'M' }), brownM)
})
