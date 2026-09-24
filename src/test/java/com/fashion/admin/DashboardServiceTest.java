package com.fashion.admin;

import com.fashion.admin.dto.DashboardDto;
import com.fashion.admin.service.DashboardService;
import com.fashion.order.repository.OrderRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetDashboardWithDefaultDates() {
        when(orderRepository.getTotalRevenueBetweenDates(org.mockito.ArgumentMatchers.any(), 
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(BigDecimal.valueOf(10000)));
        when(orderRepository.count()).thenReturn(50L);
        when(productRepository.count()).thenReturn(100L);
        when(userRepository.countByRole_NameIgnoreCase("CUSTOMER")).thenReturn(500L);
        when(productVariantRepository.sumStockQuantity()).thenReturn(1000L);
        when(orderRepository.countByCreatedAtBetween(org.mockito.ArgumentMatchers.any(), 
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(5L);

        DashboardDto dashboard = dashboardService.getDashboard(Optional.empty(), Optional.empty());

        assertNotNull(dashboard);
        assertEquals(50L, dashboard.getTotalOrders());
        assertEquals(100L, dashboard.getTotalProducts());
        assertEquals(500L, dashboard.getTotalCustomers());
    }

    @Test
    void testGetDashboardWithCustomDates() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(orderRepository.getTotalRevenueBetweenDates(from, to))
                .thenReturn(Optional.of(BigDecimal.valueOf(5000)));
        when(orderRepository.count()).thenReturn(25L);
        when(productRepository.count()).thenReturn(75L);
        when(userRepository.countByRole_NameIgnoreCase("CUSTOMER")).thenReturn(250L);
        when(productVariantRepository.sumStockQuantity()).thenReturn(1000L);
        when(orderRepository.countByCreatedAtBetween(org.mockito.ArgumentMatchers.any(), 
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(2L);

        DashboardDto dashboard = dashboardService.getDashboard(Optional.of(from), Optional.of(to));

        assertNotNull(dashboard);
        assertEquals(25L, dashboard.getTotalOrders());
        assertEquals(75L, dashboard.getTotalProducts());
    }
}
