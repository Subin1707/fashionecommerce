package com.fashion.cart.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequest {
    private Long productId;
    private Long variantId;
    private String size;
    private Integer quantity;

    public CartItemRequest() {
    }
    public CartItemRequest(final Long productId, final Long variantId, final String size, final Integer quantity) {
        this.productId = productId;
        this.variantId = variantId;
        this.size = size;
        this.quantity = quantity;
    }
}
