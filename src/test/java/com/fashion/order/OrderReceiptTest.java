package com.fashion.order;

import com.fashion.order.entity.Order;
import com.fashion.order.repository.OrderRepository;
import com.fashion.order.service.OrderService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderReceiptTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderService service = new OrderService(null, null, orders, null, null, null);

    @Test void shippingCodOrderCanBeConfirmed() {
        Order order = Order.builder().id(1L).userId(2L).orderStatus("SHIPPING").paymentMethod("COD").paymentStatus("PENDING").build();
        when(orders.findByIdAndUserId(1L, 2L)).thenReturn(Optional.of(order));
        when(orders.save(order)).thenReturn(order);
        assertThat(service.confirmReceived(1L, 2L).getOrderStatus()).isEqualTo("COMPLETED");
    }

    @Test void unpaidOnlineOrderCannotBeConfirmed() {
        Order order = Order.builder().orderStatus("SHIPPING").paymentMethod("VNPAY").paymentStatus("PENDING").build();
        when(orders.findByIdAndUserId(1L, 2L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.confirmReceived(1L, 2L)).isInstanceOf(IllegalArgumentException.class);
        verify(orders, never()).save(any());
    }

    @Test void processingOrderCannotBeConfirmed() {
        when(orders.findByIdAndUserId(1L, 2L)).thenReturn(Optional.of(Order.builder().orderStatus("PROCESSING").build()));
        assertThatThrownBy(() -> service.confirmReceived(1L, 2L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void anotherUsersOrderCannotBeConfirmed() {
        when(orders.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmReceived(1L, 2L)).isInstanceOf(IllegalArgumentException.class);
    }
}
