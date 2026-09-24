package com.fashion.shipping.controller;

import com.fashion.shipping.service.ShipmentService;
import com.fashion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
@RequiredArgsConstructor
public class CustomerShipmentController {
    private final ShipmentService service;
    @GetMapping("/{orderId}/shipment")
    public ShipmentService.TrackingView get(@PathVariable Long orderId, Authentication authentication) {
        return service.get(orderId, ((UserPrincipal) authentication.getPrincipal()).getUserId());
    }
}
