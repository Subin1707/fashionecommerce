package com.fashion.admin.service;

import com.fashion.admin.dto.AdminOrderDto;
import com.fashion.admin.dto.UpdateOrderStatusRequest;
import com.fashion.order.entity.Order;
import com.fashion.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final com.fashion.order.repository.OrderStatusHistoryRepository historyRepository;

    public Page<AdminOrderDto> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findAll(pageable).map(this::convertToDto);
    }

    public Page<AdminOrderDto> getOrdersByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findByOrderStatus(status, pageable).map(this::convertToDto);
    }

    public AdminOrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return convertToDto(order);
    }

    public AdminOrderDto updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String normalizedStatus = com.fashion.order.enums.OrderStatus.fromString(request.getOrderStatus()).name();
        String currentStatus = order.getOrderStatus();
        String newStatus = normalizedStatus;
        
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        order.setOrderStatus(newStatus);
        Order updated = orderRepository.save(order);
        historyRepository.save(com.fashion.order.entity.OrderStatusHistory.builder().orderId(orderId)
                .status(newStatus).description(switch (newStatus) {
                    case "CONFIRMED" -> "Shop đã xác nhận đơn hàng";
                    case "PROCESSING" -> "Shop đang chuẩn bị hàng";
                    case "SHIPPING" -> "Shop đã bàn giao đơn hàng cho đơn vị vận chuyển";
                    default -> "Đơn hàng đã bị hủy";
                }).build());
        return convertToDto(updated);
    }

    public List<AdminOrderDto> getOrdersForDateRange(LocalDateTime from, LocalDateTime to) {
        return orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private AdminOrderDto convertToDto(Order order) {
        return AdminOrderDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .shippingAddress(order.getShippingAddress())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .build();
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        return switch (currentStatus) {
            case "PENDING_PAYMENT" -> newStatus.equals("CANCELLED");
            case "PENDING" -> newStatus.equals("CONFIRMED") || newStatus.equals("CANCELLED");
            case "CONFIRMED" -> newStatus.equals("PROCESSING");
            case "PROCESSING" -> newStatus.equals("SHIPPING");
            default -> false;
        };
    }
}
