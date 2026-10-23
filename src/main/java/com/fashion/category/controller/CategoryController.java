package com.fashion.category.controller;

import com.fashion.category.entity.Category;
import com.fashion.category.dto.CategoryResponse;
import com.fashion.category.dto.CategoryRequest;
import com.fashion.category.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories().stream().map(CategoryResponse::from).toList());
    }

    @GetMapping("/with-products")
    public ResponseEntity<List<CategoryResponse>> getCategoriesWithProducts() {
        return ResponseEntity.ok(categoryService.getCategoriesWithSellableProducts().stream().map(CategoryResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.getCategoryById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.createCategory(request.toCategory())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.updateCategory(id, request.toCategory())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

}
