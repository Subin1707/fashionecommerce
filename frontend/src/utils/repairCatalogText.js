const decoder = new TextDecoder('utf-8', { fatal: true })
const windows1252Special = [
  0x20ac, 0x0081, 0x201a, 0x0192, 0x201e, 0x2026, 0x2020, 0x2021,
  0x02c6, 0x2030, 0x0160, 0x2039, 0x0152, 0x008d, 0x017d, 0x008f,
  0x0090, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014,
  0x02dc, 0x2122, 0x0161, 0x203a, 0x0153, 0x009d, 0x017e, 0x0178,
]
const cp1252ByteByCodePoint = new Map(windows1252Special.map((codePoint, index) => [codePoint, index + 0x80]))

function decodeMojibakePass(value) {
  const bytes = []
  for (const character of value) {
    const codePoint = character.codePointAt(0)
    if (codePoint === 0x0102) {
      bytes.push(0xc3)
    } else if (codePoint <= 0x7f || (codePoint >= 0xa0 && codePoint <= 0xff)) {
      bytes.push(codePoint)
    } else if (cp1252ByteByCodePoint.has(codePoint)) {
      bytes.push(cp1252ByteByCodePoint.get(codePoint))
    } else {
      return value
    }
  }

  try {
    return decoder.decode(new Uint8Array(bytes))
  } catch {
    return value
  }
}

export function repairCatalogText(value) {
  if (typeof value !== 'string' || !value) return value

  let repaired = value
  for (let pass = 0; pass < 3; pass += 1) {
    const next = decodeMojibakePass(repaired)
    if (next === repaired) break
    repaired = next
  }
  return repaired
}

export function repairBrand(brand) {
  if (!brand) return brand
  return {
    ...brand,
    name: repairCatalogText(brand.name),
    description: repairCatalogText(brand.description),
  }
}

export function repairCategory(category) {
  if (!category) return category
  return {
    ...category,
    name: repairCatalogText(category.name),
    description: repairCatalogText(category.description),
    parent: category.parent ? repairCategory(category.parent) : category.parent,
  }
}

export function repairProduct(product) {
  if (!product) return product
  return {
    ...product,
    name: repairCatalogText(product.name),
    shortDescription: repairCatalogText(product.shortDescription),
    description: repairCatalogText(product.description),
    brandName: repairCatalogText(product.brandName),
    categoryName: repairCatalogText(product.categoryName),
    material: repairCatalogText(product.material),
    fit: repairCatalogText(product.fit),
    variants: product.variants?.map((variant) => ({
      ...variant,
      color: repairCatalogText(variant.color),
      size: repairCatalogText(variant.size),
    })),
    colors: product.colors?.map(repairCatalogText),
    sizes: product.sizes?.map(repairCatalogText),
  }
}
