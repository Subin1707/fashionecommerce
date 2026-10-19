package com.fashion.vnpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fashion.order.entity.Order;
import com.fashion.order.repository.OrderRepository;
import com.fashion.payment.service.PaymentService;
import com.fashion.security.UserPrincipal;
import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class VNPayControllerTest {
    private final VNPayService vnPayService = mock(VNPayService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PaymentService paymentService = mock(PaymentService.class);
    private final VNPayController controller = new VNPayController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "vnPayService", vnPayService);
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "paymentService", paymentService);
    }

    @Test
    void createsPaymentUsingThePublicFrontendTunnelForCallback() {
        Order order = Order.builder()
                .id(42L)
                .userId(7L)
                .paymentMethod("VNPAY")
                .paymentStatus("PENDING")
                .finalAmount(120000d)
                .build();
        when(orderRepository.findByIdAndUserId(42L, 7L)).thenReturn(Optional.of(order));
        when(vnPayService.createOrder(anyDouble(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("https://sandbox.vnpayment.vn/payment");

        User customer = User.builder()
                .id(7L)
                .email("customer@example.com")
                .role(new Role("CUSTOMER"))
                .status("ACTIVE")
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(customer));
        MockHttpServletRequest request = tunneledRequest();

        var response = controller.createPayment(42L, authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.mockito.ArgumentCaptor<String> returnUrl = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(vnPayService).createOrder(
                eq(120000d), eq("Thanh toan don hang 42"), anyString(), returnUrl.capture(), anyString());
        assertThat(returnUrl.getValue())
                .isEqualTo("https://fashion-demo.trycloudflare.com/api/vnpay/return");
    }

    @Test
    void returnsCustomerToTheSamePublicFrontendAfterVnPayCallback() {
        when(vnPayService.isValidReturn(any(HttpServletRequest.class))).thenReturn(false);

        var response = controller.paymentReturn(tunneledRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().toString())
                .isEqualTo("https://fashion-demo.trycloudflare.com/#orders?payment=failed&orderId=");
    }

    private MockHttpServletRequest tunneledRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Host", "fashion-demo.trycloudflare.com");
        request.addHeader("X-Forwarded-Proto", "https");
        return request;
    }
}
