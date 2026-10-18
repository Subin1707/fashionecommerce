package com.fashion.admin.dto;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class UpdateOrderStatusRequest {

    private String orderStatus;

    public UpdateOrderStatusRequest() {
    }
    public UpdateOrderStatusRequest(final String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
