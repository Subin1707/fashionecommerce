package com.fashion.shipping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long providerId;

    @Column(nullable = false, unique = true, length = 100)
    private String trackingCode;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private Double shippingFee;

    @Column(nullable = false)
    private Double weightKg;

    private LocalDate estimatedDelivery;

    private Double pickupLat;

    private Double pickupLng;

    private Double deliveryLat;

    private Double deliveryLng;

    private Double currentLat;

    private Double currentLng;

    @Builder.Default
    private Integer routeIndex = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}