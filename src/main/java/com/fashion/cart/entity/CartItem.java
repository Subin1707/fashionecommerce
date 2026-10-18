package com.fashion.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private Long productId;

    @Column
    private Long variantId;

    @Column(nullable = false, length = 10)
    private String size;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal price;

    @JsonIgnore
    @Column(name = "price", nullable = false)
    private Double legacyPrice;

    public void setPrice(BigDecimal price) {
        this.price = price;
        this.legacyPrice = price == null ? null : price.doubleValue();
    }

    public void setPrice(Double price) {
        this.price = price == null ? null : BigDecimal.valueOf(price);
        this.legacyPrice = price;
    }

    public static class CartItemBuilder {
        public CartItemBuilder price(BigDecimal price) {
            this.price = price;
            this.legacyPrice = price == null ? null : price.doubleValue();
            return this;
        }

        public CartItemBuilder price(Double price) {
            this.price = price == null ? null : BigDecimal.valueOf(price);
            this.legacyPrice = price;
            return this;
        }

        public CartItemBuilder price(double price) {
            this.price = BigDecimal.valueOf(price);
            this.legacyPrice = price;
            return this;
        }

        CartItemBuilder() {
        }
    }

    @PrePersist
    @PreUpdate
    void syncLegacyPriceColumn() {
        legacyPrice = price == null ? null : price.doubleValue();
    }

    public CartItem() {
    }
    public CartItem(final Long id, final Cart cart, final Long productId, final Long variantId, final String size, final Integer quantity, final BigDecimal price, final Double legacyPrice) {
        this.id = id;
        this.cart = cart;
        this.productId = productId;
        this.variantId = variantId;
        this.size = size;
        this.quantity = quantity;
        this.price = price;
        this.legacyPrice = legacyPrice;
    }
}
