package com.fashion.voucher.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VoucherValidationResult {
    private boolean valid;
    private String code;
    private String message;
    private double discountAmount;
    private double finalAmount;

    public VoucherValidationResult() {
    }
    public VoucherValidationResult(final boolean valid, final String code, final String message, final double discountAmount, final double finalAmount) {
        this.valid = valid;
        this.code = code;
        this.message = message;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }
}
