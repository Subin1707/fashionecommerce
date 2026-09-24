package com.fashion.order.controller;

import com.fashion.order.dto.CreateOrderRequest;
import com.fashion.order.entity.Order;
import com.fashion.order.service.OrderService;
import com.fashion.payment.service.PaymentService;
import com.fashion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request, Authentication authentication) {
        return ResponseEntity.ok(orderService.createOrderFromCart(userId(authentication), request));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId(authentication)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId, Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrderByIdForUser(orderId, userId(authentication)));
    }

    @PatchMapping("/{orderId}/payment")
    public ResponseEntity<Order> completePayment(@PathVariable Long orderId, Authentication authentication) {
        return ResponseEntity.ok(paymentService.completePayment(orderId, userId(authentication)));
    }

    private Long userId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUserId();
    }

    @PatchMapping("/{orderId}/receive")
    public ResponseEntity<Order> confirmReceived(@PathVariable Long orderId, Authentication authentication) {
        return ResponseEntity.ok(orderService.confirmReceived(orderId, userId(authentication)));
    }
}
