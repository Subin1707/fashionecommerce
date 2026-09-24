package com.fashion.style.controller;

import com.fashion.product.dto.ProductResponse;
import com.fashion.style.dto.StyleMatchRequest;
import com.fashion.style.dto.VisualSearchResponse;
import com.fashion.style.service.StyleMatchService;
import com.fashion.style.service.VisualSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/style")
@RequiredArgsConstructor
public class StyleMatchController {

    private final StyleMatchService styleMatchService;
    private final VisualSearchService visualSearchService;

    @PostMapping("/recommend")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ProductResponse>> recommendProducts(@RequestBody StyleMatchRequest request) {
        return ResponseEntity.ok(styleMatchService.recommendProducts(request));
    }

    @PostMapping(value = "/visual-search", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<VisualSearchResponse> visualSearch(
            @RequestParam("image") MultipartFile image,
            @RequestParam(defaultValue = "10") int limit) {
        VisualSearchResponse response = visualSearchService.search(image, limit);
        return response.isEnabled() ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }
}
