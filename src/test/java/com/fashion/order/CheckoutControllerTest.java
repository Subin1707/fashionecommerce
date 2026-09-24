package com.fashion.order;

import com.fashion.order.controller.CheckoutController;
import com.fashion.order.dto.CheckoutRequest;
import com.fashion.order.dto.CheckoutSummary;
import com.fashion.order.service.CheckoutService;
import com.fashion.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CheckoutController checkoutController;

    @Test
    void shouldCalculateSummaryFromIncomingRequest() {
        Long userId = 42L;
        UserPrincipal principal = new UserPrincipal(com.fashion.user.entity.User.builder().id(userId).build());

        when(authentication.getPrincipal()).thenReturn(principal);

        CheckoutRequest request = new CheckoutRequest();
        request.setProductId(7L);
        request.setVariantId(9L);
        request.setQuantity(2);
        request.setVoucherCode("SAVE10");

        CheckoutSummary expected = CheckoutSummary.builder()
                .subtotal(500000.0)
                .shippingFee(30000.0)
                .discountAmount(50000.0)
                .finalAmount(280000.0)
                .build();

        when(checkoutService.calculateSummary(userId, request)).thenReturn(expected);

        ResponseEntity<CheckoutSummary> response = checkoutController.calculateSummary(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(checkoutService).calculateSummary(userId, request);
    }
}
