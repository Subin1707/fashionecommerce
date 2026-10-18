package com.fashion.shipping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shipping_providers")
@Getter
@Setter
@Builder
public class ShippingProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false, length = 20)
    private String status;

    public ShippingProvider() {
    }
    public ShippingProvider(final Long id, final String name, final String code, final String logoUrl, final String status) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.logoUrl = logoUrl;
        this.status = status;
    }
}