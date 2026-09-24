package com.fashion.product.controller;

import com.fashion.product.entity.Product;
import com.fashion.product.dto.ProductResponse;
import com.fashion.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final com.fashion.review.repository.ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts().stream().map(this::toResponse).toList());
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
        return ResponseEntity.ok(productService.getSimilarProducts(id, limit).stream().map(this::toResponse).toList());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword).stream().map(this::toResponse).toList());
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
        List<String> imageUrls = productService.getImageUrls(product.getId());
        String primaryImageUrl = imageUrls.isEmpty() ? null : imageUrls.getFirst();
        ProductResponse response = ProductResponse.fromEntity(product, primaryImageUrl, imageUrls);
        var summary = reviewRepository.summarize(product.getId());
        response.setAverageRating(summary.getAverageRating());
        response.setTotalReviews(summary.getTotalReviews());
        return response;
    }
}
