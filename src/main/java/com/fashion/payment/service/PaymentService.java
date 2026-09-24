package com.fashion.payment.service;

import com.fashion.cart.entity.Cart;
import com.fashion.cart.entity.CartItem;
import com.fashion.cart.repository.CartRepository;
import com.fashion.messaging.EventPublisher;
import com.fashion.messaging.event.PaymentCompletedEvent;
import com.fashion.order.entity.Order;
import com.fashion.order.entity.OrderItem;
import com.fashion.order.repository.OrderRepository;
import com.fashion.order.repository.OrderStatusHistoryRepository;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderStatusHistoryRepository historyRepository;

    @Transactional
    public Order completePayment(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền thanh toán đơn hàng này");
        }

        if ("COMPLETED".equalsIgnoreCase(order.getPaymentStatus())) {
            return order;
        }
        if (!"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ");
        }

        order.setPaymentStatus("PAID");
        if ("VNPAY".equalsIgnoreCase(order.getPaymentMethod())
            && "PENDING_PAYMENT".equals(order.getOrderStatus())) {
            order.setOrderStatus("CONFIRMED");
            historyRepository.save(com.fashion.order.entity.OrderStatusHistory.builder()
                .orderId(order.getId()).status("CONFIRMED")
                .description("Thanh toán VNPay thành công, đơn hàng đã được xác nhận").build());
        }
        Order saved = orderRepository.save(order);
        eventPublisher.publish("PaymentCompleted", new PaymentCompletedEvent(
                saved.getId(), saved.getUserId(), saved.getFinalAmount(),
                saved.getPaymentMethod(), Instant.now()));
        return saved;
    }

    @Transactional
    public Order completeVNPayPayment(String transactionReference, String amount) {
        final Long orderId;
        try {
            orderId = parseOrderId(transactionReference);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Mã giao dịch VNPay không hợp lệ");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
        long expectedAmount = Math.round(order.getFinalAmount() * 100);
        if (!String.valueOf(expectedAmount).equals(amount)) {
            throw new IllegalArgumentException("Số tiền thanh toán không khớp");
        }
        if ("COMPLETED".equalsIgnoreCase(order.getPaymentStatus())) {
            return order;
        }
        if (!"VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                || !"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ");
        }

        order.setPaymentStatus("PAID");
        if ("PENDING_PAYMENT".equals(order.getOrderStatus())) {
            order.setOrderStatus("CONFIRMED");
            historyRepository.save(com.fashion.order.entity.OrderStatusHistory.builder()
                .orderId(order.getId()).status("CONFIRMED")
                .description("Thanh toán VNPay thành công, đơn hàng đã được xác nhận").build());
        }
        Order saved = orderRepository.save(order);
        eventPublisher.publish("PaymentCompleted", new PaymentCompletedEvent(
                saved.getId(), saved.getUserId(), saved.getFinalAmount(),
                saved.getPaymentMethod(), Instant.now()));
        return saved;
    }

    @Transactional
    public Order cancelVNPayPayment(String transactionReference) {
        final Long orderId;
        try {
            orderId = parseOrderId(transactionReference);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Mã giao dịch VNPay không hợp lệ");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
        if ("COMPLETED".equalsIgnoreCase(order.getPaymentStatus())) {
            return order;
        }
        if ("FAILED".equalsIgnoreCase(order.getPaymentStatus())) {
            return order;
        }
        if (!"VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                || !"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ");
        }

        restoreStock(order);
        restoreCart(order);
        order.setPaymentStatus("FAILED");
        order.setOrderStatus("CANCELLED");
        return orderRepository.save(order);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = productVariantRepository.findByIdForUpdate(item.getVariantId())
                    .orElse(null);
            if (variant == null) {
                continue;
            }
            int currentStock = variant.getStockQty() == null ? 0 : variant.getStockQty();
            variant.setStockQty(currentStock + item.getQuantity());
            productVariantRepository.save(variant);
        }
    }

    private void restoreCart(Order order) {
        Cart cart = cartRepository.findByUserId(order.getUserId())
                .orElseGet(() -> Cart.builder().userId(order.getUserId()).build());

        for (OrderItem item : order.getItems()) {
            CartItem cartItem = cart.getItems().stream()
                    .filter(candidate -> Objects.equals(candidate.getVariantId(), item.getVariantId())
                            && Objects.equals(candidate.getSize(), item.getSize()))
                    .findFirst()
                    .orElseGet(() -> {
                        CartItem created = CartItem.builder()
                                .cart(cart)
                                .productId(item.getProductId())
                                .variantId(item.getVariantId())
                                .size(item.getSize())
                                .quantity(0)
                                .price(BigDecimal.valueOf(item.getPrice()))
                                .build();
                        cart.getItems().add(created);
                        return created;
                    });
            cartItem.setQuantity(cartItem.getQuantity() + item.getQuantity());
        }
        cartRepository.save(cart);
    }

    private Long parseOrderId(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new NumberFormatException("empty transaction reference");
        }
        String orderId = transactionReference.split("_", 2)[0];
        return Long.valueOf(orderId);
    }
}
