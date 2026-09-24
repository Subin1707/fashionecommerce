package com.fashion.shipping;

import com.fashion.admin.dto.UpdateOrderStatusRequest;
import com.fashion.admin.service.AdminOrderService;
import com.fashion.order.entity.Order;
import com.fashion.order.repository.OrderRepository;
import com.fashion.order.service.OrderService;
import com.fashion.shipping.entity.ShippingProvider;
import com.fashion.shipping.repository.ShippingProviderRepository;
import com.fashion.shipping.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShipmentFlowTest {
    @Autowired ShipmentService service;
    @Autowired OrderRepository orders;
    @Autowired ShippingProviderRepository providers;
    @Autowired AdminOrderService admin;
    @Autowired OrderService customer;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.fashion.shipping.service.RoutingService routing;
    @Autowired com.fashion.shipping.repository.ShipmentRepository shipments;
    @Autowired com.fashion.shipping.repository.ShipmentRoutePointRepository routePoints;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
    @Autowired org.springframework.web.context.WebApplicationContext webContext;
    Order order;
    ShippingProvider provider;

    @BeforeEach void setup() {
        org.mockito.Mockito.when(routing.findRoute(org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble())).thenReturn(new com.fashion.shipping.service.RoutingService.RouteResult(
                12000, 1800, java.util.List.of(
                    new com.fashion.shipping.service.RoutingService.RoutePoint(21.0075, 105.8412),
                    new com.fashion.shipping.service.RoutingService.RoutePoint(21.0075, 105.82),
                    new com.fashion.shipping.service.RoutingService.RoutePoint(20.99, 105.82),
                    new com.fashion.shipping.service.RoutingService.RoutePoint(20.99, 105.7872),
                    new com.fashion.shipping.service.RoutingService.RoutePoint(20.9808, 105.7872))));
        order = orders.save(Order.builder().userId(901L).orderStatus("PENDING")
                .shippingAddress("Test address").paymentMethod("COD").paymentStatus("PENDING")
                .shippingLat(20.9808).shippingLng(105.7872)
                .totalAmount(100000d).shippingFee(30000d).discountAmount(0d).finalAmount(130000d).build());
        provider = providers.save(ShippingProvider.builder().name("Test carrier").code("TEST").status("ACTIVE").build());
    }
    void prepare() {
        admin.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().orderStatus("CONFIRMED").build());
        admin.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().orderStatus("PROCESSING").build());
    }
    ShipmentService.TrackingView create() {
        return service.create(order.getId(), new ShipmentService.CreateRequest(provider.getId(), 1.2, 30000d, null));
    }
    ShipmentService.TrackingView update(String status) {
        return service.update(order.getId(), new ShipmentService.UpdateRequest(status, "Test carrier event"));
    }
    @Test void completeLifecycleAndDuplicateCallback() {
        prepare();
        var created = create();
        assertThat(created.shipment().getTrackingCode()).startsWith("TEST");
        assertThat(created.shipment().getPickupLat()).isEqualTo(21.0075);
        assertThat(created.shipment().getPickupLng()).isEqualTo(105.8412);
        assertThat(created.shipment().getDeliveryLat()).isEqualTo(20.9808);
        assertThat(created.shipment().getDeliveryLng()).isEqualTo(105.7872);
        assertThat(created.shipment().getCurrentLat()).isEqualTo(21.0075);
        assertThat(created.shipment().getCurrentLng()).isEqualTo(105.8412);
        assertThat(created.shipment().getRouteIndex()).isZero();
        assertThat(created.tracking()).extracting("status").containsExactly("CREATED", "WAITING_FOR_PICKUP");
        assertThat(order.getOrderStatus()).isEqualTo("PROCESSING");
        update("PICKED_UP");
        assertThat(order.getOrderStatus()).isEqualTo("SHIPPING");
        update("IN_TRANSIT"); update("OUT_FOR_DELIVERY"); update("DELIVERED");
        var tracked = service.get(order.getId(), 901L);
        assertThat(tracked.shipment().getCurrentLat()).isEqualTo(20.9808);
        assertThat(tracked.shipment().getCurrentLng()).isEqualTo(105.7872);
        assertThat(tracked.shipment().getRouteIndex()).isEqualTo(4);
        assertThat(tracked.tracking()).filteredOn(event -> event.getLatitude() != null)
                .isNotEmpty().allSatisfy(event -> assertThat(event.getLongitude()).isNotNull());
        int count = tracked.tracking().size();
        assertThat(update("DELIVERED").tracking()).hasSize(count);
        assertThat(order.getOrderStatus()).isEqualTo("SHIPPING");
        assertThat(customer.confirmReceived(order.getId(), 901L).getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(service.get(order.getId(), 901L).orderHistory()).extracting("status")
            .containsExactly("CONFIRMED", "PROCESSING", "SHIPPING", "COMPLETED");
    }
    @Test void simulatorFollowsStoredRouteAndStopsAtDelivery() {
        prepare();
        var view = create();
        assertThat(view.route()).hasSize(5);
        var simulator = new com.fashion.shipping.service.ShipmentSimulatorService(shipments, routePoints, orders, service, transactions);
        simulator.advance(order.getId());
        assertThat(view.shipment().getRouteIndex()).isZero();
        update("PICKED_UP");
        simulator.advance(order.getId());
        assertThat(view.shipment().getCurrentLat()).isEqualTo(21.0075);
        assertThat(view.shipment().getCurrentLng()).isEqualTo(105.82);
        assertThat(view.shipment().getStatus()).isEqualTo("IN_TRANSIT");
        for (int i = 0; i < 6; i++) simulator.advance(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo("SHIPPING");
        assertThat(view.shipment().getRouteIndex()).isEqualTo(4);
        int count = service.get(order.getId(), 901L).tracking().size();
        simulator.advance(order.getId());
        assertThat(service.get(order.getId(), 901L).tracking()).hasSize(count);
        org.mockito.Mockito.verify(routing, org.mockito.Mockito.times(1)).findRoute(21.0075, 105.8412, 20.9808, 105.7872);
    }

    @Test void shortRouteStillPassesThroughAllDeliveryStates() {
        org.mockito.Mockito.when(routing.findRoute(org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble())).thenReturn(new com.fashion.shipping.service.RoutingService.RouteResult(
                10, 2, java.util.List.of(
                    new com.fashion.shipping.service.RoutingService.RoutePoint(21.0075, 105.8412),
                    new com.fashion.shipping.service.RoutingService.RoutePoint(20.9808, 105.7872))));
        prepare(); create(); update("PICKED_UP");
        var simulator = new com.fashion.shipping.service.ShipmentSimulatorService(shipments, routePoints, orders, service, transactions);
        simulator.advance(order.getId());
        assertThat(service.get(order.getId(), 901L).shipment().getStatus()).isEqualTo("IN_TRANSIT");
        simulator.advance(order.getId());
        assertThat(service.get(order.getId(), 901L).shipment().getStatus()).isEqualTo("OUT_FOR_DELIVERY");
        simulator.advance(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo("SHIPPING");
    }

    @Test void simulatorPausesAfterFailure() {
        prepare(); create(); update("PICKED_UP"); update("IN_TRANSIT"); update("OUT_FOR_DELIVERY"); update("DELIVERY_FAILED");
        var simulator = new com.fashion.shipping.service.ShipmentSimulatorService(shipments, routePoints, orders, service, transactions);
        simulator.advance(order.getId());
        assertThat(service.get(order.getId(), 901L).shipment().getStatus()).isEqualTo("DELIVERY_FAILED");
    }

    @Test void failedDeliveryCanRetryOrReturn() {
        prepare(); create(); update("PICKED_UP"); update("IN_TRANSIT"); update("OUT_FOR_DELIVERY");
        update("DELIVERY_FAILED"); update("IN_TRANSIT"); update("OUT_FOR_DELIVERY"); update("DELIVERY_FAILED");
        assertThat(order.getOrderStatus()).isEqualTo("SHIPPING");
        update("RETURNING"); update("RETURNED");
        assertThat(order.getOrderStatus()).isEqualTo("RETURNED");
        assertThatThrownBy(() -> customer.confirmReceived(order.getId(), 901L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> update("IN_TRANSIT")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsInvalidCreationAndTransitions() {
        assertThatThrownBy(this::create).isInstanceOf(IllegalArgumentException.class);
        prepare();
        assertThatThrownBy(() -> service.create(order.getId(), new ShipmentService.CreateRequest(provider.getId(), -1d, 0d, null)))
                .isInstanceOf(IllegalArgumentException.class);
        create();
        assertThatThrownBy(this::create).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> update("DELIVERED")).isInstanceOf(IllegalArgumentException.class);
        assertThat(admin.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().orderStatus("SHIPPING").build()).getOrderStatus())
            .isEqualTo("SHIPPING");
        assertThatThrownBy(() -> admin.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().orderStatus("CANCELLED").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void requiresFailureReasonAndEnforcesOwnership() {
        prepare(); create(); update("PICKED_UP"); update("IN_TRANSIT"); update("OUT_FOR_DELIVERY");
        assertThatThrownBy(() -> service.update(order.getId(), new ShipmentService.UpdateRequest("DELIVERY_FAILED", " ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.get(order.getId(), 902L)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void httpEndpointsEnforceRolesAndOwnership() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        var principal = new com.fashion.security.UserPrincipal(com.fashion.user.entity.User.builder()
                .id(901L).email("shipping-test@example.com").password("unused")
                .role(new com.fashion.user.entity.Role("CUSTOMER")).build());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/orders/" + order.getId() + "/shipment")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/orders/" + order.getId() + "/shipment")
                .contentType("application/json").content("{}")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/shipping-providers")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        order.setUserId(902L); orders.saveAndFlush(order);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/orders/" + order.getId() + "/shipment")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }
}
