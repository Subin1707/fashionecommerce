package com.fashion.cart.service;

import com.fashion.cart.dto.CartItemRequest;
import com.fashion.cart.dto.CartItemResponse;
import com.fashion.cart.dto.CartResponse;
import com.fashion.cart.entity.Cart;
import com.fashion.cart.entity.CartItem;
import com.fashion.cart.repository.CartRepository;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductImage;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    }

    @Transactional
    public CartResponse getCartResponseByUserId(Long userId) {
        return toResponse(getCartByUserId(userId));
    }

    @Transactional
    public Cart addItemToCart(Long userId, CartItemRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng không hợp lệ");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("Sản phẩm không còn hoạt động");
        }

        var variant = product.getVariants().stream()
            .filter(v -> request.getVariantId() != null
                ? java.util.Objects.equals(v.getId(), request.getVariantId())
                : v.getSize() != null && request.getSize() != null
                    && v.getSize().equalsIgnoreCase(request.getSize()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("SKU/size không hợp lệ"));

        if (variant.getStockQty() == null || variant.getStockQty() < request.getQuantity()) {
            throw new IllegalArgumentException("Sản phẩm không đủ hàng theo SKU đã chọn");
        }

        Cart cart = getCartByUserId(userId);

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getProductId().equals(product.getId())
                        && cartItem.getVariantId() != null
                        && cartItem.getVariantId().equals(variant.getId()))
                .findFirst()
                .orElse(null);

        if (item == null) {
            BigDecimal itemPrice = product.getDisplayPrice()
                    .add(variant.getPriceAdjustment() == null ? BigDecimal.ZERO : variant.getPriceAdjustment());
            item = CartItem.builder()
                    .cart(cart)
                    .productId(product.getId())
                    .variantId(variant.getId())
                    .size(variant.getSize())
                    .quantity(request.getQuantity())
                    .price(itemPrice)
                    .build();
            cart.getItems().add(item);
        } else {
            int newTotal = item.getQuantity() + request.getQuantity();
            if (variant.getStockQty() < newTotal) {
                throw new IllegalArgumentException("Số lượng vượt quá tồn kho của SKU");
            }
            item.setQuantity(newTotal);
            BigDecimal itemPrice = product.getDisplayPrice()
                    .add(variant.getPriceAdjustment() == null ? BigDecimal.ZERO : variant.getPriceAdjustment());
            item.setPrice(itemPrice);
        }

        cartRepository.saveAndFlush(cart);
        return getCartByUserId(userId);
    }

    @Transactional
    public Cart updateCartItem(Long userId, Long itemId, CartItemRequest request) {
        Cart cart = getCartByUserId(userId);

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart item không tồn tại"));

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        if (request == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng không hợp lệ");
        }

        String requestedSize = request.getSize() == null ? item.getSize() : request.getSize();

        var variant = product.getVariants().stream()
                .filter(v -> item.getVariantId() != null
                        ? java.util.Objects.equals(v.getId(), item.getVariantId())
                        : v.getSize() != null && v.getSize().equalsIgnoreCase(requestedSize))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("SKU/size không hợp lệ"));

        if (variant.getStockQty() == null || variant.getStockQty() < request.getQuantity()) {
            throw new IllegalArgumentException("Sản phẩm không đủ hàng theo SKU đã chọn");
        }

        item.setQuantity(request.getQuantity());
        item.setVariantId(variant.getId());
        item.setSize(variant.getSize());

        BigDecimal itemPrice = product.getDisplayPrice()
                .add(variant.getPriceAdjustment() == null ? BigDecimal.ZERO : variant.getPriceAdjustment());
        item.setPrice(itemPrice);

        cartRepository.saveAndFlush(cart);
        return getCartByUserId(userId);
    }

    @Transactional
    public Cart removeCartItem(Long userId, Long itemId) {
        Cart cart = getCartByUserId(userId);
        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("Cart item không tồn tại");
        }
        cartRepository.saveAndFlush(cart);
        return getCartByUserId(userId);
    }

    @Transactional
    public Cart clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().clear();
        cartRepository.saveAndFlush(cart);
        return getCartByUserId(userId);
    }

    @Transactional(readOnly = true)
    public CartResponse toResponse(Cart cart) {
        List<CartItem> cartItems = cart.getItems();
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Product> productsById = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartItemResponse> items = cartItems.stream()
                .map(item -> CartItemResponse.fromEntity(
                        item,
                        productsById.get(item.getProductId()),
                        primaryImageUrl(item)))
                .toList();

        double total = items.stream()
                .mapToDouble(item -> item.getLineTotal() == null ? 0D : item.getLineTotal())
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .total(total)
                .build();
    }

    private String primaryImageUrl(CartItem item) {
        if (item.getProductId() == null) {
            return null;
        }

        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(item.getProductId());
        if (images.isEmpty()) {
            return null;
        }

        return images.stream()
                .filter(image -> image.getVariant() != null
                        && Objects.equals(image.getVariant().getId(), item.getVariantId()))
                .min(imageComparator())
                .or(() -> images.stream()
                        .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                        .min(imageComparator()))
                .orElse(images.getFirst())
                .getImageUrl();
    }

    private Comparator<ProductImage> imageComparator() {
        return Comparator
                .comparing((ProductImage image) -> !Boolean.TRUE.equals(image.getIsPrimary()))
                .thenComparing(image -> image.getDisplayOrder() == null ? 0 : image.getDisplayOrder())
                .thenComparing(image -> image.getId() == null ? Long.MAX_VALUE : image.getId());
    }
}
