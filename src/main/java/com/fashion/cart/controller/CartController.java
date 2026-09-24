package com.fashion.cart.controller;

import com.fashion.cart.dto.CartItemRequest;
import com.fashion.cart.dto.CartResponse;
import com.fashion.cart.service.CartService;
import com.fashion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCartResponseByUserId(customerId(authentication)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(@RequestBody CartItemRequest request, Authentication authentication) {
        return ResponseEntity.ok(cartService.toResponse(cartService.addItemToCart(customerId(authentication), request)));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateCartItem(@PathVariable Long id, @RequestBody CartItemRequest request, Authentication authentication) {
        return ResponseEntity.ok(cartService.toResponse(cartService.updateCartItem(customerId(authentication), id, request)));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<CartResponse> removeCartItem(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(cartService.toResponse(cartService.removeCartItem(customerId(authentication), id)));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.toResponse(cartService.clearCart(customerId(authentication))));
    }

    private Long customerId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUserId();
    }
}
