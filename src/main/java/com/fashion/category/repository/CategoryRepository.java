package com.fashion.category.repository;

import com.fashion.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    Optional<Category> findBySlug(String slug);

    List<Category> findByParent_Id(Long parentId);

    @EntityGraph(attributePaths = "parent")
    @Query("""
            select distinct c
            from Category c
            where c.isActive = true
              and (
                  exists (
                      select p.id from Product p
                      where p.category = c and upper(p.status) = 'ACTIVE'
                        and exists (select v.id from ProductVariant v where v.product = p and v.isActive = true and v.stockQty > 0)
                  )
                  or exists (
                      select child.id from Category child
                      where child.parent = c and child.isActive = true
                        and exists (
                            select p.id from Product p
                            where p.category = child and upper(p.status) = 'ACTIVE'
                              and exists (select v.id from ProductVariant v where v.product = p and v.isActive = true and v.stockQty > 0)
                        )
                  )
              )
            order by c.name
            """)
    List<Category> findActiveCategoriesWithSellableProducts();

    @Override
    @EntityGraph(attributePaths = "parent")
    List<Category> findAll();

    @Override
    @EntityGraph(attributePaths = "parent")
    Optional<Category> findById(Long id);
}
