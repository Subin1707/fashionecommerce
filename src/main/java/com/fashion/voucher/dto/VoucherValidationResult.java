package com.fashion.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherValidationResult {
    private boolean valid;
    private String code;
    private String message;
    private double discountAmount;
    private double finalAmount;
}
