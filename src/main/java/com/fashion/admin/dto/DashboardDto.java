package com.fashion.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
@Data
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

    public DashboardDto() {
    }
    public DashboardDto(final BigDecimal totalRevenue, final Long totalOrders, final Long totalProducts, final Long totalStock, final Long totalCustomers, final LocalDate dateFrom, final LocalDate dateTo, final BigDecimal revenueToday, final Long ordersToday, final BigDecimal averageOrderValue) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalProducts = totalProducts;
        this.totalStock = totalStock;
        this.totalCustomers = totalCustomers;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.revenueToday = revenueToday;
        this.ordersToday = ordersToday;
        this.averageOrderValue = averageOrderValue;
    }
}
