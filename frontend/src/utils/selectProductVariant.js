export function selectProductVariant(variants, selection) {
  return variants.find((variant) => (
    variant.color === selection.color && variant.size === selection.size
  ))
}
