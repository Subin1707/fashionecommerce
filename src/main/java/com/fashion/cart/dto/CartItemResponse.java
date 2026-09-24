package com.fashion.cart.dto;

import com.fashion.cart.entity.CartItem;
import com.fashion.product.entity.Product;
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
public class CartItemResponse {

    private Long id;

    private Long productId;

    private Long variantId;

    private String productName;

    private String imageUrl;

    private String size;

    private Integer quantity;

    private Double price;

    private Double lineTotal;

    public static CartItemResponse fromEntity(CartItem item) {

        double price =
                item.getPrice() == null
                        ? 0D
                        : item.getPrice().doubleValue();

        int quantity =
                item.getQuantity() == null
                        ? 0
                        : item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .size(item.getSize())
                .quantity(quantity)
                .price(price)
                .lineTotal(price * quantity)
                .build();
    }

    public static CartItemResponse fromEntity(
            CartItem item,
            Product product
    ) {
        return fromEntity(item, product, product != null ? product.getPrimaryImageUrl() : null);
    }

    public static CartItemResponse fromEntity(
            CartItem item,
            Product product,
            String imageUrl
    ) {

        double price =
                item.getPrice() == null
                        ? 0D
                        : item.getPrice().doubleValue();

        int quantity =
                item.getQuantity() == null
                        ? 0
                        : item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())

                .productName(
                        product != null
                                ? product.getName()
                                : "Sản phẩm"
                )

                .imageUrl(
                        imageUrl
                )

                .size(item.getSize())
                .quantity(quantity)
                .price(price)
                .lineTotal(price * quantity)
                .build();
    }
}
