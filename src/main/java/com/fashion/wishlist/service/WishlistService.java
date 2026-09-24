package com.fashion.wishlist.service;

import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.wishlist.entity.WishlistItem;
import com.fashion.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<WishlistItem> getWishlist(Long userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public WishlistItem addToWishlist(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("Sản phẩm không còn hoạt động");
        }

        if (wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new IllegalArgumentException("Sản phẩm đã có trong wishlist");
        }

        WishlistItem wishlistItem = WishlistItem.builder()
                .userId(userId)
                .productId(product.getId())
                .build();

        return wishlistRepository.save(wishlistItem);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không có trong wishlist"));

        wishlistRepository.delete(item);
    }
}
