package com.fashion.product.controller;

import com.fashion.product.entity.Product;
import com.fashion.product.dto.ProductResponse;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.service.ProductService;
import com.fashion.review.repository.ReviewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(toResponses(productService.getAllProducts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(toResponse(product));
    }

    @GetMapping("/{id}/similar")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ProductResponse>> getSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(toResponses(productService.getSimilarProducts(id, limit)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(toResponses(productService.getProductsByCategory(categoryId)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(toResponses(productService.searchProducts(keyword)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private ProductResponse toResponse(Product product) {
        return toResponses(List.of(product)).getFirst();
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductImageRepository.ProductImageSummary>> imagesByProduct = new HashMap<>();
        Map<Long, ReviewRepository.ProductRatingSummary> ratingsByProduct = new HashMap<>();
        for (int start = 0; start < productIds.size(); start += 500) {
            List<Long> batch = productIds.subList(start, Math.min(start + 500, productIds.size()));
            productImageRepository.findSummariesByProductIdIn(batch).forEach(image ->
                    imagesByProduct.computeIfAbsent(image.getProductId(), ignored -> new ArrayList<>()).add(image));
            reviewRepository.summarizeProducts(batch).forEach(rating ->
                    ratingsByProduct.put(rating.getProductId(), rating));
        }

        return products.stream()
                .map(product -> {
                    List<String> imageUrls = getImageUrls(imagesByProduct.get(product.getId()));
                    ProductResponse response = ProductResponse.fromEntity(
                            product,
                            imageUrls.isEmpty() ? null : imageUrls.getFirst(),
                            imageUrls);
                    ReviewRepository.ProductRatingSummary rating = ratingsByProduct.get(product.getId());
                    response.setAverageRating(rating == null ? 0.0 : rating.getAverageRating());
                    response.setTotalReviews(rating == null ? 0L : rating.getTotalReviews());
                    return response;
                })
                .toList();
    }

    private List<String> getImageUrls(List<ProductImageRepository.ProductImageSummary> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        ProductImageRepository.ProductImageSummary primaryImage = images.stream()
                .filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
                .filter(image -> image.getVariantId() == null)
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .min(Comparator
                        .comparing((ProductImageRepository.ProductImageSummary image) ->
                                image.getDisplayOrder() == null ? 0 : image.getDisplayOrder())
                        .thenComparing(image -> image.getId() == null ? Long.MAX_VALUE : image.getId()))
                .orElseGet(() -> images.stream()
                        .filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
                        .filter(image -> image.getVariantId() == null)
                        .findFirst()
                        .orElseGet(() -> images.stream()
                                .filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
                                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                                .findFirst()
                                .orElseGet(() -> images.stream()
                                        .filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
                                        .findFirst()
                                        .orElse(null))));

        if (primaryImage == null) {
            return List.of();
        }

        return images.stream()
                .sorted(Comparator
                        .comparing((ProductImageRepository.ProductImageSummary image) -> image != primaryImage)
                        .thenComparing(image -> image.getDisplayOrder() == null ? 0 : image.getDisplayOrder())
                        .thenComparing(image -> image.getId() == null ? Long.MAX_VALUE : image.getId()))
                .map(ProductImageRepository.ProductImageSummary::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();
    }

    public ProductController(
            ProductService productService,
            ProductImageRepository productImageRepository,
            ReviewRepository reviewRepository) {
        this.productService = productService;
        this.productImageRepository = productImageRepository;
        this.reviewRepository = reviewRepository;
    }
}
