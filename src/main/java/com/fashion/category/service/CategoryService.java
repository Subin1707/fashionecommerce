package com.fashion.category.service;

import com.fashion.category.entity.Category;
import com.fashion.category.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryList", key = "'all'")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesWithSellableProducts() {
        return categoryRepository.findActiveCategoriesWithSellableProducts();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "#id")
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    @Transactional
    @CacheEvict(cacheNames = {"categories", "categoryList"}, allEntries = true)
    public Category createCategory(Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new RuntimeException("Category already exists: " + category.getName());
        }
        if (category.getParent() != null && category.getParent().getId() != null) {
            category.setParent(categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại")));
        }
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }
        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(cacheNames = {"categories", "categoryList"}, allEntries = true)
    public Category updateCategory(Long id, Category category) {
        Category existing = getCategoryById(id);
        if (category.getName() != null && !category.getName().isBlank()) {
            categoryRepository.findByName(category.getName().trim())
                    .filter(match -> !match.getId().equals(id))
                    .ifPresent(match -> { throw new IllegalArgumentException("Danh mục đã tồn tại: " + category.getName()); });
        }
        existing.setName(category.getName());
        if (category.getSlug() != null && !category.getSlug().isBlank()) {
            existing.setSlug(category.getSlug());
        }
        existing.setDescription(category.getDescription());
        if (category.getIsActive() != null) {
            existing.setIsActive(category.getIsActive());
        }
        if (category.getParent() != null && category.getParent().getId() != null) {
            if (category.getParent().getId().equals(id)) {
                throw new RuntimeException("A category cannot be its own parent");
            }
            existing.setParent(categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại")));
        } else {
            existing.setParent(null);
        }
        return categoryRepository.save(existing);
    }

    @Transactional
    @CacheEvict(cacheNames = {"categories", "categoryList"}, allEntries = true)
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    public CategoryService(final CategoryRepository categoryRepository) {
        this.categoryRepository = java.util.Objects.requireNonNull(categoryRepository, "categoryRepository must not be null");
    }
}
