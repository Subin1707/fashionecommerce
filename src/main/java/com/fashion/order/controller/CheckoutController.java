package com.fashion.order.controller;

import com.fashion.order.dto.CheckoutRequest;
import com.fashion.order.dto.CheckoutSummary;
import com.fashion.order.entity.Order;
import com.fashion.order.service.CheckoutService;
import com.fashion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @GetMapping("/summary")
    public ResponseEntity<CheckoutSummary> calculateSummary(Authentication authentication,
                                                           @RequestBody(required = false) CheckoutRequest request) {
        CheckoutRequest normalizedRequest = request == null ? new CheckoutRequest() : request;
        return ResponseEntity.ok(checkoutService.calculateSummary(userId(authentication), normalizedRequest));
    }

    @PostMapping
    public ResponseEntity<Order> checkout(@RequestBody CheckoutRequest request, Authentication authentication) {
        return ResponseEntity.ok(checkoutService.checkout(userId(authentication), request));
    }

    private Long userId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUserId();
    }
}
