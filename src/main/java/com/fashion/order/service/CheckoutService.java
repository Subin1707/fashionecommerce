package com.fashion.order.service;

import com.fashion.cart.entity.Cart;
import com.fashion.cart.entity.CartItem;
import com.fashion.cart.repository.CartRepository;
import com.fashion.messaging.EventPublisher;
import com.fashion.messaging.event.OrderCreatedEvent;
import com.fashion.messaging.event.OrderItemEvent;
import com.fashion.messaging.event.StockUpdatedEvent;
import com.fashion.order.dto.CheckoutRequest;
import com.fashion.order.dto.CheckoutSummary;
import com.fashion.order.entity.Order;
import com.fashion.order.entity.OrderItem;
import com.fashion.order.enums.OrderStatus;
import com.fashion.order.repository.OrderRepository;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.voucher.dto.VoucherValidationResult;
import com.fashion.voucher.service.VoucherService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final double SHIPPING_FEE = 30000.0;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final com.fashion.order.repository.OrderStatusHistoryRepository historyRepository;
    private final EventPublisher eventPublisher;
    private final VoucherService voucherService;

    @Transactional(readOnly = true)
    public CheckoutSummary calculateSummary(Long userId, CheckoutRequest request) {
        if (isBuyNow(request)) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + request.getProductId()));
            ProductVariant variant = findRequestVariant(product, request);
            int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
            if (quantity <= 0) {
                throw new IllegalArgumentException("Số lượng không hợp lệ");
            }

            double subtotal = calculateUnitPrice(product, variant) * quantity;
            double shippingFee = SHIPPING_FEE;
            double discountAmount = request == null ? 0.0 : calculateDiscount(request.getVoucherCode(), subtotal);
            double finalAmount = subtotal + shippingFee - discountAmount;

            return CheckoutSummary.builder()
                    .subtotal(subtotal)
                    .shippingFee(shippingFee)
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .build();
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng trống"));

        double subtotal = 0.0;
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + item.getProductId()));
            ProductVariant variant = findCartVariant(product, item);
            subtotal += calculateUnitPrice(product, variant) * item.getQuantity();
            }
        }

        double shippingFee = SHIPPING_FEE;
        double discountAmount = request == null ? 0.0 : calculateDiscount(request.getVoucherCode(), subtotal);
        double finalAmount = subtotal + shippingFee - discountAmount;

        return CheckoutSummary.builder()
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    @Transactional
    public Order checkout(Long userId, CheckoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu checkout không hợp lệ");
        }

        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không hợp lệ");
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        }

        if (isBuyNow(request)) {
            return checkoutBuyNow(userId, request);
        }

        List<Cart> carts = cartRepository.findAllByUserId(userId);
        if (carts.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        Cart cart = carts.getFirst();
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        double subtotal = 0.0;
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + cartItem.getProductId()));

            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new IllegalArgumentException("Sản phẩm không còn hoạt động: " + product.getName());
            }

            ProductVariant variant = findCartVariant(product, cartItem);
            if (variant.getStockQty() == null || variant.getStockQty() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Sản phẩm không đủ hàng: " + product.getName());
            }

                subtotal += calculateUnitPrice(product, variant) * cartItem.getQuantity();
        }

        double shippingFee = SHIPPING_FEE;
        double discountAmount = calculateDiscount(request.getVoucherCode(), subtotal);
        double finalAmount = subtotal + shippingFee - discountAmount;

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .shippingAddress(request.getShippingAddress())
                .shippingLat(request.getShippingLat())
                .shippingLng(request.getShippingLng())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("PENDING")
                .couponCode(normalizeCouponCode(request.getVoucherCode()))
                .orderStatus("VNPAY".equalsIgnoreCase(request.getPaymentMethod())
                    ? OrderStatus.PENDING_PAYMENT.name() : OrderStatus.PENDING.name())
                .build();

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + cartItem.getProductId()));
                ProductVariant variant = productVariantRepository.findByIdForUpdate(cartItem.getVariantId())
                    .filter(candidate -> candidate.getProduct().getId().equals(product.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("SKU không tồn tại trong giỏ hàng"));
                if (variant.getStockQty() == null || variant.getStockQty() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Sản phẩm không đủ hàng: " + product.getName());
                }
                double unitPrice = calculateUnitPrice(product, variant);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .productName(product.getName())
                    .color(variant.getColor())
                    .size(cartItem.getSize())
                    .quantity(cartItem.getQuantity())
                    .price(unitPrice)
                    .build();

            order.getItems().add(orderItem);

            if (variant.getStockQty() != null) {
                variant.setStockQty(Math.max(0, variant.getStockQty() - cartItem.getQuantity()));
                eventPublisher.publish("StockUpdated", new StockUpdatedEvent(
                        product.getId(), variant.getId(), -cartItem.getQuantity(),
                        variant.getStockQty(), "CHECKOUT", Instant.now()));
            }
            productRepository.save(product);
        }

        orderRepository.save(order);
        historyRepository.save(com.fashion.order.entity.OrderStatusHistory.builder().orderId(order.getId())
                .status("PENDING").description("Đơn hàng đã được đặt thành công").build());
        eventPublisher.publish("OrderCreated", new OrderCreatedEvent(
                order.getId(), order.getUserId(), order.getTotalAmount(), order.getFinalAmount(),
                order.getPaymentMethod(), order.getItems().stream()
                        .map(item -> new OrderItemEvent(item.getProductId(), item.getVariantId(),
                                item.getSku(), item.getQuantity(), item.getPrice()))
                        .collect(Collectors.toList()), Instant.now()));
        cart.getItems().clear();
        cartRepository.save(cart);

        return order;
    }

    private Order checkoutBuyNow(Long userId, CheckoutRequest request) {
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng không hợp lệ");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + request.getProductId()));

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("Sản phẩm không còn hoạt động: " + product.getName());
        }

        ProductVariant variant = productVariantRepository.findByIdForUpdate(request.getVariantId())
                .filter(candidate -> candidate.getProduct().getId().equals(product.getId()))
                .orElseThrow(() -> new IllegalArgumentException("SKU không tồn tại"));

        if (variant.getStockQty() == null || variant.getStockQty() < quantity) {
            throw new IllegalArgumentException("Sản phẩm không đủ hàng: " + product.getName());
        }

        double unitPrice = calculateUnitPrice(product, variant);
        double subtotal = unitPrice * quantity;
        double shippingFee = SHIPPING_FEE;
        double discountAmount = calculateDiscount(request.getVoucherCode(), subtotal);
        double finalAmount = subtotal + shippingFee - discountAmount;

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .shippingAddress(request.getShippingAddress())
                .shippingLat(request.getShippingLat())
                .shippingLng(request.getShippingLng())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("PENDING")
                .couponCode(normalizeCouponCode(request.getVoucherCode()))
                .orderStatus("VNPAY".equalsIgnoreCase(request.getPaymentMethod())
                    ? OrderStatus.PENDING_PAYMENT.name() : OrderStatus.PENDING.name())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product.getName())
                .color(variant.getColor())
                .size(variant.getSize())
                .quantity(quantity)
                .price(unitPrice)
                .build();

        order.getItems().add(orderItem);
        variant.setStockQty(Math.max(0, variant.getStockQty() - quantity));
        productRepository.save(product);

        orderRepository.save(order);
        historyRepository.save(com.fashion.order.entity.OrderStatusHistory.builder().orderId(order.getId())
                .status("PENDING").description("Đơn hàng đã được đặt thành công").build());
        eventPublisher.publish("StockUpdated", new StockUpdatedEvent(
                product.getId(), variant.getId(), -quantity,
                variant.getStockQty(), "BUY_NOW", Instant.now()));
        eventPublisher.publish("OrderCreated", new OrderCreatedEvent(
                order.getId(), order.getUserId(), order.getTotalAmount(), order.getFinalAmount(),
                order.getPaymentMethod(), order.getItems().stream()
                        .map(item -> new OrderItemEvent(item.getProductId(), item.getVariantId(),
                                item.getSku(), item.getQuantity(), item.getPrice()))
                        .collect(Collectors.toList()), Instant.now()));

        return order;
    }

    private boolean isBuyNow(CheckoutRequest request) {
        return request != null && request.getProductId() != null && request.getVariantId() != null;
    }

    private ProductVariant findRequestVariant(Product product, CheckoutRequest request) {
        return product.getVariants().stream()
                .filter(variant -> Objects.equals(variant.getId(), request.getVariantId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("SKU không tồn tại"));
    }

    private ProductVariant findCartVariant(Product product, CartItem cartItem) {
        return product.getVariants().stream()
                .filter(variant -> cartItem.getVariantId() != null
                        ? Objects.equals(variant.getId(), cartItem.getVariantId())
                        : variant.getSize() != null && variant.getSize().equalsIgnoreCase(cartItem.getSize()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("SKU không tồn tại trong giỏ hàng"));
    }

    private double calculateUnitPrice(Product product, ProductVariant variant) {
        return product.getDisplayPrice()
                .add(variant.getPriceAdjustment() == null ? java.math.BigDecimal.ZERO : variant.getPriceAdjustment())
                .doubleValue();
    }

    private double calculateDiscount(String voucherCode, double subtotal) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return 0.0;
        }

        VoucherValidationResult result = voucherService.validateVoucher(voucherCode, subtotal);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getMessage());
        }
        return Math.min(Math.max(result.getDiscountAmount(), 0.0), subtotal);
    }

    private String normalizeCouponCode(String couponCode) {
        return couponCode == null || couponCode.isBlank() ? null : couponCode.trim().toUpperCase();
    }
}
