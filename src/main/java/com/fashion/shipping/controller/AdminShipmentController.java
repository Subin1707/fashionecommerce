package com.fashion.shipping.controller;

import com.fashion.shipping.service.ShipmentService;
import com.fashion.shipping.entity.ShippingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminShipmentController {
    private final ShipmentService service;
    @GetMapping("/shipping-providers")
    public List<ShippingProvider> providers() { return service.providers(); }
    @GetMapping("/orders/{orderId}/shipment")
    public ShipmentService.TrackingView get(@PathVariable Long orderId) { return service.get(orderId, null); }
    @PostMapping("/orders/{orderId}/shipment")
    public ShipmentService.TrackingView create(@PathVariable Long orderId, @RequestBody ShipmentService.CreateRequest request) {
        return service.create(orderId, request);
    }
    @PatchMapping("/orders/{orderId}/shipment/simulate")
    public ShipmentService.TrackingView simulate(@PathVariable Long orderId, @RequestBody ShipmentService.UpdateRequest request) {
        return service.update(orderId, request);
    }
}
