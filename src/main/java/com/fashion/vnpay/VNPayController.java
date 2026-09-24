package com.fashion.vnpay;

import com.fashion.order.entity.Order;
import com.fashion.order.repository.OrderRepository;
import com.fashion.payment.service.PaymentService;
import com.fashion.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Value("${vnpay.frontend-return-url:http://localhost:5173/orders}")
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

        String returnUrl = VNPayConfig.vnp_Returnurl;
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

        String result = frontendReturnUrl + "?payment=" + (paid ? "success" : "failed")
                + "&orderId=" + extractOrderId(txnRef);
        return ResponseEntity.status(302).header("Location", result).build();
    }

    private String extractOrderId(String transactionReference) {
        if (transactionReference == null) {
            return "";
        }
        int separator = transactionReference.indexOf('_');
        return separator < 0 ? transactionReference : transactionReference.substring(0, separator);
    }
}