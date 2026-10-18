package com.fashion.shipping.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipment_route_points")
@Getter @Setter @Builder
public class ShipmentRoutePoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long shipmentId;
    @Column(nullable = false) private Integer pointIndex;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;

    public ShipmentRoutePoint() {
    }
    public ShipmentRoutePoint(final Long id, final Long shipmentId, final Integer pointIndex, final double latitude, final double longitude) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.pointIndex = pointIndex;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
