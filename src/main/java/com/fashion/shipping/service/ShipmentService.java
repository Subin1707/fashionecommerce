package com.fashion.shipping.service;

import com.fashion.order.entity.*;
import com.fashion.order.repository.*;
import com.fashion.shipping.entity.*;
import com.fashion.shipping.enums.ShipmentStatus;
import com.fashion.shipping.repository.*;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentService {
    private final OrderRepository orders;
    private final ShipmentRepository shipments;
    private final ShippingProviderRepository providers;
    private final ShipmentTrackingRepository tracking;
    private final OrderStatusHistoryRepository history;
    private final ShippingGateway gateway;
    private final RoutingService routing;
    private final ShipmentRoutePointRepository routePoints;

    public record CreateRequest(Long providerId, Double weightKg, Double shippingFee, LocalDate estimatedDelivery) {}
    public record UpdateRequest(String status, String description) {}
    public record TrackingView(Shipment shipment, ShippingProvider provider,
            List<ShipmentTracking> tracking, List<OrderStatusHistory> orderHistory, List<RoutingService.RoutePoint> route) {}

    @Transactional(readOnly = true)
    public List<ShippingProvider> providers() { return providers.findByStatusOrderByNameAsc("ACTIVE"); }

    @Transactional(readOnly = true)
    public TrackingView get(Long orderId, Long customerId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
        if (customerId != null && !customerId.equals(order.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền xem đơn hàng");
        }
        Shipment shipment = shipments.findByOrderId(orderId).orElse(null);
        return new TrackingView(shipment, shipment == null ? null : providers.findById(shipment.getProviderId()).orElse(null),
                shipment == null ? List.of() : tracking.findByShipmentIdOrderByCreatedAtAscIdAsc(shipment.getId()),
                history.findByOrderIdOrderByCreatedAtAscIdAsc(orderId),
                shipment == null ? List.of() : routePoints.findByShipmentIdOrderByPointIndexAsc(shipment.getId()).stream()
                    .map(p -> new RoutingService.RoutePoint(p.getLatitude(), p.getLongitude())).toList());
    }

    public TrackingView create(Long orderId, CreateRequest request) {
        Order order = lockOrder(orderId);
        if (!"PROCESSING".equals(order.getOrderStatus())) throw new IllegalArgumentException("Chỉ tạo vận đơn khi đang chuẩn bị hàng");
        if (shipments.findByOrderId(orderId).isPresent()) throw new IllegalArgumentException("Đơn hàng đã có vận đơn");
        validateCoordinates(order.getShippingLat(), order.getShippingLng());
        if (request == null || request.providerId() == null || request.weightKg() == null
                || !Double.isFinite(request.weightKg()) || request.weightKg() <= 0
                || request.shippingFee() == null || !Double.isFinite(request.shippingFee()) || request.shippingFee() < 0) {
            throw new IllegalArgumentException("Đơn vị vận chuyển, khối lượng và phí vận chuyển không hợp lệ");
        }
        if (request.estimatedDelivery() != null && request.estimatedDelivery().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ngày giao dự kiến không được ở quá khứ");
        ShippingProvider provider = providers.findById(request.providerId())
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Đơn vị vận chuyển không hoạt động"));
        Shipment shipment = shipments.save(Shipment.builder().orderId(orderId).providerId(provider.getId())
                .trackingCode(gateway.createTrackingCode(provider, order, request.weightKg(), request.shippingFee()))
                .weightKg(request.weightKg()).shippingFee(request.shippingFee()).estimatedDelivery(request.estimatedDelivery())
            .pickupLat(21.0075)
            .pickupLng(105.8412)
                .deliveryLat(order.getShippingLat())
                .deliveryLng(order.getShippingLng())
            .currentLat(21.0075)
            .currentLng(105.8412)
            .routeIndex(0)
                .status("CREATED").build());
        storeRoute(shipment);
        ShippingGateway.RoutePoint start = gateway.route(shipment).getFirst();
        shipment.setCurrentLat(start.latitude());
        shipment.setCurrentLng(start.longitude());
        record(shipment, "Đã tạo yêu cầu vận chuyển", start);
        shipment.setStatus("WAITING_FOR_PICKUP");
        shipments.save(shipment);
        record(shipment, "Chờ đơn vị vận chuyển lấy hàng", start);
        return get(orderId, null);
    }

    /** Called by the admin simulator; a verified carrier callback can reuse this transaction later. */
    public TrackingView update(Long orderId, UpdateRequest request) {
        Order order = lockOrder(orderId);
        Shipment shipment = shipments.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng chưa có vận đơn"));
        if (request == null) throw new IllegalArgumentException("Thiếu trạng thái vận chuyển");
        ShipmentStatus target = ShipmentStatus.fromString(request.status());
        ShipmentStatus current = ShipmentStatus.fromString(shipment.getStatus());
        if (current == target) return get(orderId, null); // carrier retries do not duplicate history
        if (!next(current).contains(target)) throw new IllegalArgumentException("Không thể chuyển vận chuyển từ " + current + " sang " + target);
        if (!List.of("PROCESSING", "SHIPPING").contains(order.getOrderStatus()))
            throw new IllegalArgumentException("Đơn hàng không còn trong quá trình vận chuyển");
        String description = request.description() == null ? "" : request.description().trim();
        if (description.length() > 500)
            throw new IllegalArgumentException("Mô tả quá dài");
        if (target == ShipmentStatus.DELIVERY_FAILED && description.isEmpty())
            throw new IllegalArgumentException("Vui lòng nhập lý do giao thất bại");
        var route = gateway.route(shipment);
        if (route.isEmpty()) {
            storeRoute(shipment); // Upgrade legacy shipments on an explicit admin action, never on polling.
            route = gateway.route(shipment);
            shipment.setRouteIndex(0);
        }
        int index = shipment.getRouteIndex();
        if (target == ShipmentStatus.DELIVERED) index = route.size() - 1;
        if (target == ShipmentStatus.RETURNED) index = 0;
        ShippingGateway.RoutePoint point = route.get(Math.min(index, route.size() - 1));
        shipment.setRouteIndex(index);
        shipment.setCurrentLat(point.latitude());
        shipment.setCurrentLng(point.longitude());
        shipment.setStatus(target.name());
        shipments.save(shipment);
        record(shipment, description.isEmpty() ? label(target) : description, point);
        String orderStatus = switch(target) {
            case PICKED_UP -> "SHIPPING";
            case RETURNED -> "RETURNED";
            default -> order.getOrderStatus();
        };
        if (!orderStatus.equals(order.getOrderStatus())) {
            order.setOrderStatus(orderStatus);
            orders.save(order);
            history.save(OrderStatusHistory.builder().orderId(orderId).status(orderStatus).description(label(target)).build());
        }
        return get(orderId, null);
    }

    private void storeRoute(Shipment shipment) {
        validateCoordinates(shipment.getPickupLat(), shipment.getPickupLng());
        validateCoordinates(shipment.getDeliveryLat(), shipment.getDeliveryLng());
        var route = routing.findRoute(shipment.getPickupLat(), shipment.getPickupLng(),
                shipment.getDeliveryLat(), shipment.getDeliveryLng());
        for (int i = 0; i < route.points().size(); i++) {
            var point = route.points().get(i);
            routePoints.save(ShipmentRoutePoint.builder().shipmentId(shipment.getId()).pointIndex(i)
                    .latitude(point.latitude()).longitude(point.longitude()).build());
        }
    }

    private Order lockOrder(Long id) {
        return orders.findForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Tọa độ giao hàng không hợp lệ");
        }
    }
    private void record(Shipment shipment, String description, ShippingGateway.RoutePoint point) {
        tracking.save(ShipmentTracking.builder().shipmentId(shipment.getId()).status(shipment.getStatus())
                .description(description).location(point == null ? null : point.location())
                .latitude(point == null ? null : point.latitude())
                .longitude(point == null ? null : point.longitude()).build());
    }
    public static List<ShipmentStatus> next(ShipmentStatus status) {
        return switch(status) {
            case CREATED -> List.of(ShipmentStatus.WAITING_FOR_PICKUP);
            case WAITING_FOR_PICKUP -> List.of(ShipmentStatus.PICKED_UP);
            case PICKED_UP -> List.of(ShipmentStatus.IN_TRANSIT);
            case IN_TRANSIT -> List.of(ShipmentStatus.OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> List.of(ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERY_FAILED);
            case DELIVERY_FAILED -> List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.RETURNING);
            case RETURNING -> List.of(ShipmentStatus.RETURNED);
            case DELIVERED, RETURNED -> List.of();
        };
    }
    private static String label(ShipmentStatus status) {
        return switch(status) {
            case CREATED -> "Đã tạo vận đơn";
            case WAITING_FOR_PICKUP -> "Chờ lấy hàng";
            case PICKED_UP -> "Đơn vị vận chuyển đã lấy hàng";
            case IN_TRANSIT -> "Đơn hàng đang được trung chuyển";
            case OUT_FOR_DELIVERY -> "Đơn hàng đang được giao tới bạn";
            case DELIVERED -> "Giao hàng thành công";
            case DELIVERY_FAILED -> "Giao hàng không thành công";
            case RETURNING -> "Đang hoàn hàng về shop";
            case RETURNED -> "Đã hoàn hàng về shop";
        };
    }
}
