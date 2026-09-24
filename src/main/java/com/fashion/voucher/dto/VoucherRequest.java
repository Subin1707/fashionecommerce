package com.fashion.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRequest {
    private String code;
    private String discountType;
    private Double discountValue;
    private Double minOrderValue;
    private Double maxDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
}
