package com.fashion.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "product_variants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "color", "size"})
)
@Getter
@Setter
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 80)
    private String color;

    @Column(nullable = false, length = 20)
    private String size;

    @Default
    @Column(nullable = false)
    private Integer stockQty = 0;

    @Default
    @Column(precision = 12, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ProductVariant() {
        this.stockQty = ProductVariant.$default$stockQty();
        this.priceAdjustment = ProductVariant.$default$priceAdjustment();
        this.isActive = ProductVariant.$default$isActive();
    }
    public ProductVariant(final Long id, final Product product, final String sku, final String color, final String size, final Integer stockQty, final BigDecimal priceAdjustment, final Boolean isActive, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.product = product;
        this.sku = sku;
        this.color = color;
        this.size = size;
        this.stockQty = stockQty;
        this.priceAdjustment = priceAdjustment;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
