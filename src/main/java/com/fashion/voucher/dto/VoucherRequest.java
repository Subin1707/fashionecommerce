package com.fashion.voucher.dto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherRequest {
    private String code;
    private String discountType;
    private Double discountValue;
    private Double minOrderValue;
    private Double maxDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;

    public VoucherRequest() {
    }
    public VoucherRequest(final String code, final String discountType, final Double discountValue, final Double minOrderValue, final Double maxDiscount, final LocalDateTime startDate, final LocalDateTime endDate, final String status) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxDiscount = maxDiscount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }
}
