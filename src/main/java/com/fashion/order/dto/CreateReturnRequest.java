package com.fashion.order.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateReturnRequest {
    @NotNull private Long orderItemId;
    @NotBlank @Pattern(regexp = "RETURN|EXCHANGE") private String requestType;
    @NotNull @Min(1) private Integer quantity;
    @NotBlank @Size(max = 500) private String reason;
}
