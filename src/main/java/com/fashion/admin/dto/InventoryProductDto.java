package com.fashion.admin.dto;

public record InventoryProductDto(
        Long id,
        String name,
        String slug,
        String brandName,
        String categoryName,
        Long totalStock) {
}
