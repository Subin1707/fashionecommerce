package com.fashion.smartsize.controller;

import com.fashion.smartsize.dto.SmartSizeRequest;
import com.fashion.smartsize.dto.SmartSizeResult;
import com.fashion.smartsize.service.SmartSizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/smart-size")
@RequiredArgsConstructor
public class SmartSizeController {

    private final SmartSizeService smartSizeService;

    @PostMapping("/products/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SmartSizeResult> recommendSize(@PathVariable Long productId,
                                                        @RequestBody SmartSizeRequest request) {
        return ResponseEntity.ok(smartSizeService.recommendSize(productId, request));
    }
}
