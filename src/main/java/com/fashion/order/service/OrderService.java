package com.fashion.order.service;

import com.fashion.cart.entity.Cart;
import com.fashion.cart.entity.CartItem;
import com.fashion.cart.repository.CartRepository;
import com.fashion.order.dto.CreateOrderRequest;
import com.fashion.order.entity.Order;
import com.fashion.order.entity.OrderItem;
import com.fashion.order.entity.OrderStatusHistory;
import com.fashion.order.enums.OrderStatus;
import com.fashion.order.repository.OrderRepository;
import com.fashion.messaging.EventPublisher;
import com.fashion.messaging.event.OrderCreatedEvent;
import com.fashion.messaging.event.OrderItemEvent;
import com.fashion.messaging.event.StockUpdatedEvent;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.voucher.dto.VoucherValidationResult;
import com.fashion.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final double SHIPPING_FEE = 30000.0;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final com.fashion.order.repository.OrderStatusHistoryRepository historyRepository;
    private final EventPublisher eventPublisher;
    private final VoucherService voucherService;

    @Transactional
    public Order createOrderFromCart(Long userId, CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu đặt hàng không hợp lệ");
        }

        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không hợp lệ");
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        }

        List<Cart> carts = cartRepository.findAllByUserId(userId);
        if (carts.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        Cart cart = carts.getFirst();
        if (carts.size() > 1) {
            for (int i = 1; i < carts.size(); i++) {
                Cart duplicateCart = carts.get(i);
                if (duplicateCart.getItems() != null) {
                    for (CartItem duplicateItem : duplicateCart.getItems()) {
                        cart.getItems().add(duplicateItem);
                        duplicateItem.setCart(cart);
                    }
                }
            }
        }

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        double totalAmount = 0.0;

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + cartItem.getProductId()));

            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new IllegalArgumentException("Sản phẩm không còn hoạt động: " + product.getName());
            }

            if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ");
            }

            var variant = product.getVariants().stream()
                    .filter(v -> Objects.equals(v.getId(), cartItem.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("SKU không tồn tại trong giỏ hàng"));

            if (variant.getStockQty() == null || variant.getStockQty() < cartItem.getQuantity()) {
                throw new IllegalArgumentException("Sản phẩm không đủ hàng: " + product.getName());
            }

            double unitPrice = product.getDisplayPrice()
                    .add(variant.getPriceAdjustment() == null ? java.math.BigDecimal.ZERO : variant.getPriceAdjustment())
                    .doubleValue();
            totalAmount += unitPrice * cartItem.getQuantity();
        }

        double shippingFee = SHIPPING_FEE;
        double discountAmount = calculateDiscount(request.getVoucherCode(), totalAmount);
        double finalAmount = totalAmount + shippingFee - discountAmount;

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .shippingAddress(request.getShippingAddress())
                .shippingLat(request.getShippingLat())
                .shippingLng(request.getShippingLng())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("PENDING")
                .couponCode(request.getVoucherCode() == null || request.getVoucherCode().isBlank()
                    ? null : request.getVoucherCode().trim().toUpperCase())
                .orderStatus("PENDING")
                .build();

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + cartItem.getProductId()));

            var variant = product.getVariants().stream()
                    .filter(v -> Objects.equals(v.getId(), cartItem.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("SKU không tồn tại trong giỏ hàng"));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .productName(product.getName())
                    .color(variant.getColor())
                    .size(cartItem.getSize())
                    .quantity(cartItem.getQuantity())
                        .price(product.getDisplayPrice()
                            .add(variant.getPriceAdjustment() == null ? java.math.BigDecimal.ZERO : variant.getPriceAdjustment())
                            .doubleValue())
                    .build();

            if (order.getItems() == null) {
                order.setItems(new java.util.ArrayList<>());
            }
            order.getItems().add(orderItem);

            if (variant.getStockQty() != null) {
                variant.setStockQty(Math.max(0, variant.getStockQty() - cartItem.getQuantity()));
                eventPublisher.publish("StockUpdated", new StockUpdatedEvent(
                        product.getId(), variant.getId(), -cartItem.getQuantity(),
                        variant.getStockQty(), "ORDER_CREATED", Instant.now()));
            }
            productRepository.save(product);
        }

        orderRepository.save(order);
        recordStatus(order.getId(), OrderStatus.PENDING, "Đơn hàng đã được đặt thành công");
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

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Order getOrderByIdForUser(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        OrderStatus targetStatus = OrderStatus.fromString(status);
        OrderStatus currentStatus = OrderStatus.valueOf(order.getOrderStatus());

        boolean validTransition = isValidTransition(currentStatus, targetStatus);
        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Không thể chuyển trạng thái từ " + currentStatus + " sang " + targetStatus);
        }

        order.setOrderStatus(targetStatus.name());
        orderRepository.save(order);
        recordStatus(order.getId(), targetStatus, statusDescription(targetStatus));
        return order;
    }

    private void recordStatus(Long orderId, OrderStatus status, String description) {
        if (historyRepository != null) {
            historyRepository.save(OrderStatusHistory.builder()
                    .orderId(orderId)
                    .status(status.name())
                    .description(description)
                    .build());
        }
    }

    private String statusDescription(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "Đơn hàng đang chờ thanh toán";
            case PENDING -> "Đơn hàng đã được đặt thành công";
            case CONFIRMED -> "Shop đã xác nhận đơn hàng";
            case PROCESSING -> "Shop đang chuẩn bị hàng";
            case SHIPPING -> "Đơn hàng đang được giao";
            case DELIVERED -> "Đơn hàng đã được giao thành công";
            case COMPLETED -> "Khách hàng đã xác nhận nhận hàng";
            case RETURNED -> "Đơn hàng đã hoàn về shop";
            case CANCELLED -> "Đơn hàng đã bị hủy";
        };
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus target) {
        if (current == target) {
            return true;
        }

        switch (current) {
            case PENDING -> {
                return target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            }
            case CONFIRMED -> {
                return target == OrderStatus.PROCESSING;
            }
            case PROCESSING -> {
                return target == OrderStatus.SHIPPING;
            }
            case SHIPPING -> {
                return target == OrderStatus.COMPLETED;
            }
            case DELIVERED -> {
                return false;
            }
            case COMPLETED, CANCELLED, RETURNED -> {
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @Transactional
    public Order confirmReceived(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
        if ("COMPLETED".equals(order.getOrderStatus())) return order;
        if (!"SHIPPING".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Chỉ xác nhận nhận hàng khi đơn đang được vận chuyển");
        }
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())
            && (order.getPaymentStatus() == null || !List.of("PAID", "COMPLETED").contains(order.getPaymentStatus()))) {
            throw new IllegalArgumentException("Đơn hàng chưa được xác nhận thanh toán");
        }
        order.setOrderStatus("COMPLETED");
        order.setCompletedAt(java.time.LocalDateTime.now());
        recordStatus(order.getId(), OrderStatus.COMPLETED, statusDescription(OrderStatus.COMPLETED));
        return orderRepository.save(order);
    }
}
