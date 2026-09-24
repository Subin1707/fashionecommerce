package com.fashion.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long totalProducts;
    private Long totalStock;
    private Long totalCustomers;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal revenueToday;
    private Long ordersToday;
    private BigDecimal averageOrderValue;
}
