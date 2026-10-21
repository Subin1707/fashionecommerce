package com.fashion.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long variantId;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 80)
    private String color;

    @Column(nullable = false, length = 20)
    private String size;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Double vatAmount;

    public OrderItem() {
    }
    public OrderItem(final Long id, final Order order, final Long productId, final Long variantId, final String sku, final String productName, final String color, final String size, final Integer quantity, final Double price, final Double vatAmount) {
        this.id = id;
        this.order = order;
        this.productId = productId;
        this.variantId = variantId;
        this.sku = sku;
        this.productName = productName;
        this.color = color;
        this.size = size;
        this.quantity = quantity;
        this.price = price;
        this.vatAmount = vatAmount;
    }
}
