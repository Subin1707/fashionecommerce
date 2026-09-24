package com.fashion.search.controller;

import com.fashion.search.dto.ProductSearchRequest;
import com.fashion.search.dto.ProductSearchResponse;
import com.fashion.search.service.ProductSearchService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> syncProductsToElasticsearch() {
        productSearchService.syncProductsToElasticsearch();
        return ResponseEntity.ok("Products synced to Elasticsearch");
    }

    @GetMapping
    public ResponseEntity<Page<ProductSearchResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size_param) {
        
        ProductSearchRequest request = ProductSearchRequest.builder()
                .keyword(keyword)
                .category(category)
                .style(style)
                .size(size)
                .color(color)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .build();
        
        Page<ProductSearchResponse> results = productSearchService.searchProducts(request, page, size_param);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/top-rated")
    public ResponseEntity<Page<ProductSearchResponse>> getTopRatedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProductSearchResponse> results = productSearchService.getTopRatedProducts(page, size);
        return ResponseEntity.ok(results);
    }
}
