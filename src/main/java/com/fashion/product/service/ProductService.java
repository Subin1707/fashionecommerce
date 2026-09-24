package com.fashion.product.service;

import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductImage;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;
import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public String getPrimaryImageUrl(Long productId) {
        List<String> imageUrls = getImageUrls(productId);
        return imageUrls.isEmpty() ? null : imageUrls.getFirst();
    }

    @Transactional(readOnly = true)
    public List<String> getImageUrls(Long productId) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        if (images.isEmpty()) {
            return List.of();
        }

        ProductImage primaryImage = images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .min(Comparator
                        .comparing((ProductImage image) -> image.getDisplayOrder() == null ? 0 : image.getDisplayOrder())
                        .thenComparing(image -> image.getId() == null ? Long.MAX_VALUE : image.getId()))
                .orElse(images.getFirst());

        return images.stream()
                .sorted(Comparator
                        .comparing((ProductImage image) -> image != primaryImage)
                        .thenComparing(image -> image.getDisplayOrder() == null ? 0 : image.getDisplayOrder())
                        .thenComparing(image -> image.getId() == null ? Long.MAX_VALUE : image.getId()))
                .map(ProductImage::getImageUrl)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategory_Id(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        return query.isEmpty() ? productRepository.findAll()
            : productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
    }

    @Transactional(readOnly = true)
    public List<Product> getSimilarProducts(Long productId, int limit) {
        Product source = getProductById(productId);
        BigDecimal sourcePrice = source.getDisplayPrice();
        int resultLimit = Math.max(1, Math.min(limit, 10));

        return productRepository.findAll().stream()
                .filter(product -> !product.getId().equals(source.getId()))
                .filter(product -> "ACTIVE".equalsIgnoreCase(product.getStatus()))
                .filter(product -> product.getAvailableQuantity() > 0)
                .map(product -> new SimilarProduct(product, similarityScore(source, product, sourcePrice)))
                .sorted(Comparator.comparingInt(SimilarProduct::score).reversed()
                        .thenComparing(item -> item.product().getId()))
                .limit(resultLimit)
                .map(SimilarProduct::product)
                .toList();
    }

    private int similarityScore(Product source, Product candidate, BigDecimal sourcePrice) {
        int score = 0;
        String sourceCategory = source.getCategory() == null ? null : source.getCategory().getName();
        String candidateCategory = candidate.getCategory() == null ? null : candidate.getCategory().getName();
        if (same(sourceCategory, candidateCategory)) score += 40;
        if (same(source.getGender(), candidate.getGender())) score += 20;
        if (same(source.getBrandName(), candidate.getBrandName())) score += 10;
        if (same(source.getMaterial(), candidate.getMaterial())) score += 10;
        if (same(source.getFit(), candidate.getFit())) score += 5;
        if (isPriceNearby(sourcePrice, candidate.getDisplayPrice())) score += 15;
        return score;
    }

    private boolean same(String left, String right) {
        return left != null && right != null
                && !left.isBlank() && left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isPriceNearby(BigDecimal sourcePrice, BigDecimal candidatePrice) {
        if (sourcePrice == null || candidatePrice == null || sourcePrice.signum() <= 0) return false;
        BigDecimal difference = sourcePrice.subtract(candidatePrice).abs();
        return difference.divide(sourcePrice, 4, java.math.RoundingMode.HALF_UP).doubleValue() <= 0.30;
    }

    private record SimilarProduct(Product product, int score) { }

    @Transactional
    @CacheEvict(cacheNames = {"products", "productList"}, allEntries = true)
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(cacheNames = {"products", "productList"}, allEntries = true)
    public Product updateProduct(Long id, Product product) {
        Product existing = getProductById(id);
        existing.setName(product.getName());
        existing.setSlug(product.getSlug());
        existing.setShortDescription(product.getShortDescription());
        existing.setDescription(product.getDescription());
        existing.setBasePrice(product.getBasePrice());
        existing.setSalePrice(product.getSalePrice());
        existing.setMaterial(product.getMaterial());
        existing.setFit(product.getFit());
        existing.setGender(product.getGender());
        existing.setCategory(product.getCategory());
        existing.setBrand(product.getBrand());
        existing.setStatus(product.getStatus());
        existing.setIsFeatured(product.getIsFeatured());
        existing.setIsNew(product.getIsNew());
        return productRepository.save(existing);
    }

    @Transactional
    @CacheEvict(cacheNames = {"products", "productList"}, allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}
