package com.fashion.category.dto;

import com.fashion.category.entity.Category;
import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        Long parentId,
        String parentName,
        Boolean isActive,
        LocalDateTime createdAt) {

    public static CategoryResponse from(Category category) {
        Category parent = category.getParent();
        return new CategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                parent == null ? null : parent.getId(), parent == null ? null : parent.getName(),
                category.getIsActive(), category.getCreatedAt());
    }
}
