package com.fashion.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashion.voucher.entity.Voucher;
import com.fashion.voucher.repository.VoucherRepository;
import com.fashion.voucher.service.VoucherService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoucherServiceUnitTest {

    @Mock VoucherRepository voucherRepository;
    @InjectMocks VoucherService voucherService;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsBlankCodeWithoutQueryingRepository(String code) {
        var result = voucherService.validateVoucher(code, 100000.0);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getDiscountAmount()).isZero();
        assertThat(result.getFinalAmount()).isEqualTo(100000.0);
        verifyNoInteractions(voucherRepository);
    }

    @Test
    void rejectsUnknownCodeAndNormalizesInput() {
        when(voucherRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        var result = voucherService.validateVoucher(" missing ", 100000.0);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getCode()).isEqualTo("MISSING");
        assertThat(result.getDiscountAmount()).isZero();
        assertThat(result.getFinalAmount()).isEqualTo(100000.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVE", "EXPIRED", "FUTURE", "NO_START", "NO_END", "ZERO_DISCOUNT", "NO_DISCOUNT"})
    void rejectsUnavailableVoucher(String scenario) {
        Voucher voucher = activeVoucher();
        switch (scenario) {
            case "INACTIVE" -> voucher.setStatus("INACTIVE");
            case "EXPIRED" -> voucher.setEndDate(LocalDateTime.now().minusDays(1));
            case "FUTURE" -> voucher.setStartDate(LocalDateTime.now().plusDays(1));
            case "NO_START" -> voucher.setStartDate(null);
            case "NO_END" -> voucher.setEndDate(null);
            case "ZERO_DISCOUNT" -> voucher.setDiscountValue(0.0);
            case "NO_DISCOUNT" -> voucher.setDiscountValue(null);
        }
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

        var result = voucherService.validateVoucher("SALE10", 100000.0);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getDiscountAmount()).isZero();
        assertThat(result.getFinalAmount()).isEqualTo(100000.0);
    }

    @Test
    void rejectsOrderBelowMinimum() {
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(activeVoucher()));

        var result = voucherService.validateVoucher("SALE10", 99999.0);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getDiscountAmount()).isZero();
        assertThat(result.getFinalAmount()).isEqualTo(99999.0);
    }

    @Test
    void acceptsOrderExactlyAtMinimum() {
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(activeVoucher()));

        var result = voucherService.validateVoucher("SALE10", 100000.0);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualTo(10000.0);
        assertThat(result.getFinalAmount()).isEqualTo(90000.0);
    }

    @Test
    void fixedDiscountCannotMakeFinalAmountNegative() {
        Voucher voucher = activeVoucher();
        voucher.setDiscountType("FIXED_AMOUNT");
        voucher.setDiscountValue(150000.0);
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

        var result = voucherService.validateVoucher("SALE10", 100000.0);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFinalAmount()).isZero();
    }

    private Voucher activeVoucher() {
        return Voucher.builder().code("SALE10").discountType("PERCENTAGE")
                .discountValue(10.0).minOrderValue(100000.0).status("ACTIVE")
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(2)).build();
    }

    @Test
    void validatesPercentageVoucherAndCapsDiscount() {
        Voucher voucher = Voucher.builder()
                .code("SALE10").discountType("PERCENTAGE").discountValue(10.0)
                .minOrderValue(100000.0).maxDiscount(50000.0).status("ACTIVE")
                .startDate(LocalDateTime.now().minusDays(1)).endDate(LocalDateTime.now().plusDays(1))
                .build();
        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

        var result = voucherService.validateVoucher("sale10", 1000000.0);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountAmount()).isEqualTo(50000.0);
        assertThat(result.getFinalAmount()).isEqualTo(950000.0);
    }
}
