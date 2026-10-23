package com.fashion.category.dto;

import com.fashion.category.entity.Category;

public record CategoryRequest(
        String name,
        String slug,
        String description,
        Long parentId,
        Boolean isActive) {

    public Category toCategory() {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setIsActive(isActive);
        if (parentId != null) {
            Category parent = new Category();
            parent.setId(parentId);
            category.setParent(parent);
        }
        return category;
    }
}
