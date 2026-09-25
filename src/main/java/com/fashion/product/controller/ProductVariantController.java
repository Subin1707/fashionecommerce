package com.fashion.product.controller;

import com.fashion.product.entity.ProductVariant;
import com.fashion.product.dto.ProductVariantResponse;
import com.fashion.product.dto.CreateVariantRequest;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService productVariantService;
    private final ProductRepository productRepository;

    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantResponse>> getVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(productVariantService.getVariantsByProductId(productId).stream()
                .map(ProductVariantResponse::fromEntity).toList());
    }

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> createVariant(@PathVariable Long productId, @RequestBody CreateVariantRequest request) {
        if (request == null || request.getColor() == null || request.getColor().isBlank()) {
            throw new IllegalArgumentException("Color không hợp lệ");
        }
        if (request.getSize() == null || request.getSize().isBlank()) {
            throw new IllegalArgumentException("Size không hợp lệ");
        }
        if (request.getStockQty() == null || request.getStockQty() < 0) {
            throw new IllegalArgumentException("Stock không hợp lệ");
        }
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        ProductVariant variant = ProductVariant.builder()
                .sku(String.format("SKU-%d-%s-%s", productId,
                        request.getColor().trim().substring(0, 1).toUpperCase(), request.getSize().trim()))
                .color(request.getColor())
                .size(request.getSize())
                .stockQty(request.getStockQty())
                .priceAdjustment(request.getPriceAdjustment())
                .product(product)
                .build();
        return ResponseEntity.ok(ProductVariantResponse.fromEntity(productVariantService.createVariant(variant)));
    }
}
