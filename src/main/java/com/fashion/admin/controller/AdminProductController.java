package com.fashion.admin.controller;

import com.fashion.admin.dto.AdminProductDto;
import com.fashion.admin.dto.AdminProductVariantDto;
import com.fashion.admin.dto.CreateProductRequest;
import com.fashion.admin.dto.CreateVariantRequest;
import com.fashion.admin.dto.UpdateProductRequest;
import com.fashion.admin.dto.UpdateVariantRequest;
import com.fashion.admin.service.AdminProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<Page<AdminProductDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AdminProductDto> products = adminProductService.getAllProducts(page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<AdminProductDto> getProductById(@PathVariable Long productId) {
        AdminProductDto product = adminProductService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<AdminProductDto> createProduct(@RequestBody CreateProductRequest request) {
        AdminProductDto product = adminProductService.createProduct(request);
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<AdminProductDto> updateProduct(
            @PathVariable Long productId,
            @RequestBody UpdateProductRequest request) {
        AdminProductDto product = adminProductService.updateProduct(productId, request);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        adminProductService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<AdminProductVariantDto>> getProductVariants(@PathVariable Long productId) {
        List<AdminProductVariantDto> variants = adminProductService.getProductVariants(productId);
        return ResponseEntity.ok(variants);
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<AdminProductVariantDto> createVariant(
            @PathVariable Long productId,
            @RequestBody CreateVariantRequest request) {
        AdminProductVariantDto variant = adminProductService.createVariant(productId, request);
        return ResponseEntity.ok(variant);
    }

    @PutMapping("/variants/{variantId}")
    public ResponseEntity<AdminProductVariantDto> updateVariant(
            @PathVariable Long variantId,
            @RequestBody UpdateVariantRequest request) {
        AdminProductVariantDto variant = adminProductService.updateVariant(variantId, request);
        return ResponseEntity.ok(variant);
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long variantId) {
        adminProductService.deleteVariant(variantId);
        return ResponseEntity.noContent().build();
    }
}
