package com.fashion.category.repository;

import com.fashion.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    Optional<Category> findBySlug(String slug);

    @Override
    @EntityGraph(attributePaths = "parent")
    List<Category> findAll();

    @Override
    @EntityGraph(attributePaths = "parent")
    Optional<Category> findById(Long id);
}
