package com.fashion.wishlist.controller;

import com.fashion.wishlist.entity.WishlistItem;
import com.fashion.wishlist.service.WishlistService;
import com.fashion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItem>> getWishlist(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getWishlist(userId(authentication)));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistItem> addToWishlist(@PathVariable Long productId, Authentication authentication) {
        return ResponseEntity.ok(wishlistService.addToWishlist(userId(authentication), productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId, Authentication authentication) {
        wishlistService.removeFromWishlist(userId(authentication), productId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUserId();
    }
}
