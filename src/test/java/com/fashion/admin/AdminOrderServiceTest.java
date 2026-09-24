package com.fashion.admin;

import com.fashion.admin.dto.AdminOrderDto;
import com.fashion.admin.dto.UpdateOrderStatusRequest;
import com.fashion.admin.service.AdminOrderService;
import com.fashion.order.entity.Order;
import com.fashion.order.repository.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private com.fashion.order.repository.OrderStatusHistoryRepository historyRepository;

    @InjectMocks
    private AdminOrderService adminOrderService;

    @Test
    void testGetOrderById() {
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .userId(1L)
                .shippingAddress("123 Test St")
                .totalAmount(100.00)
                .shippingFee(10.00)
                .discountAmount(0.00)
                .finalAmount(110.00)
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .orderStatus("PENDING")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        AdminOrderDto result = adminOrderService.getOrderById(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals("PENDING", result.getOrderStatus());
        assertEquals("COD", result.getPaymentMethod());
    }

    @Test
    void testUpdateOrderStatusValidTransition() {
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .userId(1L)
                .shippingAddress("123 Test St")
                .totalAmount(100.00)
                .shippingFee(10.00)
                .discountAmount(0.00)
                .finalAmount(110.00)
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .orderStatus("PENDING")
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .orderStatus("CONFIRMED")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        AdminOrderDto result = adminOrderService.updateOrderStatus(orderId, request);

        assertNotNull(result);
        assertEquals("CONFIRMED", result.getOrderStatus());
    }

    @Test
    void testUpdateOrderStatusInvalidTransition() {
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .userId(1L)
                .shippingAddress("123 Test St")
                .totalAmount(100.00)
                .shippingFee(10.00)
                .discountAmount(0.00)
                .finalAmount(110.00)
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .orderStatus("PENDING")
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .orderStatus("SHIPPING")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(RuntimeException.class, () -> adminOrderService.updateOrderStatus(orderId, request));
    }

    @Test
    void testUpdateOrderStatusToCancelled() {
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .userId(1L)
                .shippingAddress("123 Test St")
                .totalAmount(100.00)
                .shippingFee(10.00)
                .discountAmount(0.00)
                .finalAmount(110.00)
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .orderStatus("PENDING")
                .build();

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .orderStatus("CANCELLED")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        AdminOrderDto result = adminOrderService.updateOrderStatus(orderId, request);

        assertNotNull(result);
        assertEquals("CANCELLED", result.getOrderStatus());
    }
}
