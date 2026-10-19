package com.fashion.vnpay;

import com.fashion.order.entity.Order;
import com.fashion.order.repository.OrderRepository;
import com.fashion.payment.service.PaymentService;
import com.fashion.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;

@RestController
@RequestMapping("/api/vnpay")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentService paymentService;

    @Value("${vnpay.public-base-url:}")
    private String publicBaseUrl;

    @Value("${vnpay.return-url:}")
    private String configuredReturnUrl;

    @Value("${vnpay.frontend-return-url:}")
    private String frontendReturnUrl;

    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<Map<String, String>> createPayment(@PathVariable Long orderId,
                                                               Authentication authentication,
                                                               HttpServletRequest request) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        if (!"VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new IllegalArgumentException("Đơn hàng không sử dụng VNPay");
        }
        if (!"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalArgumentException("Đơn hàng không ở trạng thái chờ thanh toán");
        }

        String returnUrl = configuredReturnUrl == null || configuredReturnUrl.isBlank()
                ? publicBaseUrl(request) + "/api/vnpay/return"
                : configuredReturnUrl.trim();
        String transactionReference = orderId + "_" + Instant.now().toEpochMilli();
        String url = vnPayService.createOrder(order.getFinalAmount(), "Thanh toan don hang " + orderId,
            transactionReference, returnUrl, VNPayConfig.getIpAddress(request));
        return ResponseEntity.ok(Map.of("paymentUrl", url));
    }

    @GetMapping("/return")
    public ResponseEntity<Void> paymentReturn(HttpServletRequest request) {
        boolean valid = vnPayService.isValidReturn(request);
        String txnRef = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        String transactionStatus = request.getParameter("vnp_TransactionStatus");
        String amount = request.getParameter("vnp_Amount");

        boolean paid = valid && "00".equals(responseCode) && "00".equals(transactionStatus);
        if (paid) {
            try {
                paymentService.completeVNPayPayment(txnRef, amount);
            } catch (IllegalArgumentException exception) {
                paid = false;
            }
        } else if (valid && txnRef != null) {
            try {
                paymentService.cancelVNPayPayment(txnRef);
            } catch (IllegalArgumentException exception) {
                // Keep the redirect usable even if VNPay sends an incomplete reference.
            }
        }

        String destination = frontendReturnUrl == null || frontendReturnUrl.isBlank()
                ? publicBaseUrl(request) + "/#orders"
                : frontendReturnUrl.trim();
        String result = destination + "?payment=" + (paid ? "success" : "failed")
                + "&orderId=" + extractOrderId(txnRef);
        return ResponseEntity.status(302).header("Location", result).build();
    }

    private String publicBaseUrl(HttpServletRequest request) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String configured = publicBaseUrl.trim().replaceAll("/+$", "");
            URI uri = URI.create(configured);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalStateException("VNPAY_PUBLIC_BASE_URL must be an HTTP(S) origin.");
            }
            return configured;
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("Public payment URL is missing. Configure VNPAY_PUBLIC_BASE_URL.");
        }
        host = host.split(",", 2)[0].trim();

        boolean localHost = host.matches("(?i)(localhost|127\\.0\\.0\\.1)(:\\d{1,5})?");
        boolean cloudflareQuickTunnel = host.matches("(?i)[a-z0-9-]+\\.trycloudflare\\.com");
        if (!localHost && !cloudflareQuickTunnel) {
            throw new IllegalStateException(
                    "Configure VNPAY_PUBLIC_BASE_URL for this public host before starting a payment.");
        }

        String protocol = request.getHeader("X-Forwarded-Proto");
        if (protocol == null || protocol.isBlank()) {
            protocol = cloudflareQuickTunnel ? "https" : request.getScheme();
        }
        protocol = protocol.split(",", 2)[0].trim().toLowerCase(java.util.Locale.ROOT);
        if (!"https".equals(protocol) && !"http".equals(protocol)) {
            throw new IllegalStateException("The forwarded payment URL protocol is invalid.");
        }
        return protocol + "://" + host;
    }

    private String extractOrderId(String transactionReference) {
        if (transactionReference == null) {
            return "";
        }
        int separator = transactionReference.indexOf('_');
        return separator < 0 ? transactionReference : transactionReference.substring(0, separator);
    }
}