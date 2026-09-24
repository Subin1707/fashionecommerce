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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "product_size_charts",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_size_chart", columnNames = {"product_id", "size_label"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSizeChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "size_label", nullable = false, length = 20)
    private String sizeLabel;

    @Column(name = "min_chest", nullable = false)
    private Double minChest;

    @Column(name = "max_chest", nullable = false)
    private Double maxChest;

    @Column(name = "min_waist", nullable = false)
    private Double minWaist;

    @Column(name = "max_waist", nullable = false)
    private Double maxWaist;

    @Column(name = "min_hip", nullable = false)
    private Double minHip;

    @Column(name = "max_hip", nullable = false)
    private Double maxHip;
}
