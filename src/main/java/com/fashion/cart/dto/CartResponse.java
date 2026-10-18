package com.fashion.cart.dto;

import com.fashion.cart.entity.Cart;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CartResponse {

    private Long id;

    private Long userId;

    private List<CartItemResponse> items;

    private Double total;

    public static CartResponse fromEntity(Cart cart) {

        List<CartItemResponse> items =
                cart.getItems()
                        .stream()
                        .map(CartItemResponse::fromEntity)
                        .toList();

        double total =
                items.stream()
                        .mapToDouble(
                                item ->
                                        item.getLineTotal() == null
                                                ? 0D
                                                : item.getLineTotal()
                        )
                        .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .total(total)
                .build();
    }

    public CartResponse() {
    }
    public CartResponse(final Long id, final Long userId, final List<CartItemResponse> items, final Double total) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.total = total;
    }
}