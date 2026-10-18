package com.fashion.product.entity;

import com.fashion.brand.entity.Brand;
import com.fashion.category.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(length = 150)
    private String material;

    @Column(length = 100)
    private String fit;

    @Column(length = 30)
    private String gender;

    @Column(nullable = false, length = 30)
    @Default
    private String status = "ACTIVE";

    @Column(nullable = false)
    @Default
    private Boolean isFeatured = false;

    @Column(nullable = false)
    @Default
    private Boolean isNew = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product")
    @Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    @Default
    private List<ProductImage> images = new ArrayList<>();

    public Integer getAvailableQuantity() {
        if (variants == null || variants.isEmpty()) {
            return 0;
        }
        return variants.stream()
                .map(ProductVariant::getStockQty)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public BigDecimal getDisplayPrice() {
        return salePrice != null ? salePrice : basePrice;
    }

    @JsonIgnore
    public String getBrandName() {
        return brand != null ? brand.getName() : "";
    }

    @JsonIgnore
    public String getPrimaryImageUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(images.get(0).getImageUrl());
    }

    public Product() {
        this.status = Product.$default$status();
        this.isFeatured = Product.$default$isFeatured();
        this.isNew = Product.$default$isNew();
        this.variants = Product.$default$variants();
        this.images = Product.$default$images();
    }
    public Product(final Long id, final Brand brand, final Category category, final String name, final String slug, final String shortDescription, final String description, final BigDecimal basePrice, final BigDecimal salePrice, final String material, final String fit, final String gender, final String status, final Boolean isFeatured, final Boolean isNew, final LocalDateTime createdAt, final LocalDateTime updatedAt, final List<ProductVariant> variants, final List<ProductImage> images) {
        this.id = id;
        this.brand = brand;
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.basePrice = basePrice;
        this.salePrice = salePrice;
        this.material = material;
        this.fit = fit;
        this.gender = gender;
        this.status = status;
        this.isFeatured = isFeatured;
        this.isNew = isNew;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.variants = variants;
        this.images = images;
    }
}
