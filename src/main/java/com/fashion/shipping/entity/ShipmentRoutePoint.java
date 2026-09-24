package com.fashion.shipping.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipment_route_points")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ShipmentRoutePoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long shipmentId;
    @Column(nullable = false) private Integer pointIndex;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;
}
