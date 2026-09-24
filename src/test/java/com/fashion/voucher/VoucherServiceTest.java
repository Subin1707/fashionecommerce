package com.fashion.voucher;

import com.fashion.voucher.dto.VoucherRequest;
import com.fashion.voucher.dto.VoucherValidationResult;
import com.fashion.voucher.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VoucherServiceTest {

    @Autowired
    private VoucherService voucherService;

    @Test
    void shouldApplyPercentageVoucherWhenOrderMeetsMinimum() {
        VoucherRequest request = new VoucherRequest();
        request.setCode("NQSALE");
        request.setDiscountType("PERCENTAGE");
        request.setDiscountValue(10.0);
        request.setMinOrderValue(100000.0);
        request.setMaxDiscount(50000.0);
        request.setStatus("ACTIVE");

        voucherService.createVoucher(request);

        VoucherValidationResult result = voucherService.validateVoucher("NQSALE", 500000.0);
        assertThat(result.getDiscountAmount()).isEqualTo(50000.0);
        assertThat(result.getFinalAmount()).isEqualTo(450000.0);
    }
}
