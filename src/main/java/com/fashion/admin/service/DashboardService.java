package com.fashion.admin.service;

import com.fashion.admin.dto.DashboardDto;
import com.fashion.order.repository.OrderRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public DashboardDto getDashboard(Optional<LocalDateTime> dateFrom, Optional<LocalDateTime> dateTo) {
        LocalDateTime fromDate = dateFrom.orElseGet(() -> LocalDateTime.now().minusMonths(1));
        LocalDateTime toDate = dateTo.orElseGet(() -> LocalDateTime.now().with(LocalTime.MAX));

        BigDecimal totalRevenue = orderRepository.getTotalRevenueBetweenDates(fromDate, toDate)
                .orElse(BigDecimal.ZERO);

        Long totalOrders = orderRepository.count();
        Long totalProducts = productRepository.count();
        Long totalCustomers = userRepository.countByRole_NameIgnoreCase("CUSTOMER");

        Long totalStock = productVariantRepository.sumStockQuantity();

        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.now().with(LocalTime.MAX);

        BigDecimal revenueToday = orderRepository.getTotalRevenueBetweenDates(todayStart, todayEnd)
                .orElse(BigDecimal.ZERO);

        Long ordersToday = orderRepository.countByCreatedAtBetween(todayStart, todayEnd);

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders))
                : BigDecimal.ZERO;

        return DashboardDto.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .totalCustomers(totalCustomers)
                .totalStock(totalStock)
                .revenueToday(revenueToday)
                .ordersToday(ordersToday)
                .averageOrderValue(averageOrderValue)
                .dateFrom(fromDate.toLocalDate())
                .dateTo(toDate.toLocalDate())
                .build();
    }
}
