package com.fashion.product.repository;

import com.fashion.product.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Override
    @EntityGraph(attributePaths = {"brand", "category", "variants"})
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = {"brand", "category", "variants"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"brand", "category", "variants"})
    List<Product> findByCategory_Id(Long categoryId);

    @EntityGraph(attributePaths = {"brand", "category", "variants"})
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"brand", "category", "variants"})
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

    @EntityGraph(attributePaths = {"brand", "category", "variants"})
    @Query(
            value = """
                    select distinct p
                    from Product p
                    join p.brand b
                    join p.category c
                    where upper(p.status) = 'ACTIVE'
                      and (:keyword is null
                           or lower(p.name) like :keyword
                           or lower(p.description) like :keyword
                           or lower(p.material) like :keyword
                           or lower(b.name) like :keyword
                           or lower(c.name) like :keyword)
                      and (:category is null
                           or lower(c.slug) = :category
                           or lower(c.name) like :categoryPattern)
                      and (:style is null
                           or lower(p.name) like :style
                           or lower(p.description) like :style
                           or lower(p.fit) like :style
                           or lower(p.material) like :style
                           or lower(p.gender) like :style
                           or lower(c.name) like :style)
                      and (:size is null
                           or exists (
                               select 1
                               from ProductVariant sizeVariant
                               where sizeVariant.product = p
                                 and sizeVariant.isActive = true
                                 and lower(sizeVariant.size) = :size
                           ))
                      and (:color is null
                           or exists (
                               select 1
                               from ProductVariant colorVariant
                               where colorVariant.product = p
                                 and colorVariant.isActive = true
                                 and lower(colorVariant.color) = :color
                           ))
                      and (:minPrice is null or coalesce(p.salePrice, p.basePrice) >= :minPrice)
                      and (:maxPrice is null or coalesce(p.salePrice, p.basePrice) <= :maxPrice)
                    order by p.isFeatured desc, p.isNew desc, p.id asc
                    """,
            countQuery = """
                    select count(distinct p)
                    from Product p
                    join p.brand b
                    join p.category c
                    where upper(p.status) = 'ACTIVE'
                      and (:keyword is null
                           or lower(p.name) like :keyword
                           or lower(p.description) like :keyword
                           or lower(p.material) like :keyword
                           or lower(b.name) like :keyword
                           or lower(c.name) like :keyword)
                      and (:category is null
                           or lower(c.slug) = :category
                           or lower(c.name) like :categoryPattern)
                      and (:style is null
                           or lower(p.name) like :style
                           or lower(p.description) like :style
                           or lower(p.fit) like :style
                           or lower(p.material) like :style
                           or lower(p.gender) like :style
                           or lower(c.name) like :style)
                      and (:size is null
                           or exists (
                               select 1
                               from ProductVariant sizeVariant
                               where sizeVariant.product = p
                                 and sizeVariant.isActive = true
                                 and lower(sizeVariant.size) = :size
                           ))
                      and (:color is null
                           or exists (
                               select 1
                               from ProductVariant colorVariant
                               where colorVariant.product = p
                                 and colorVariant.isActive = true
                                 and lower(colorVariant.color) = :color
                           ))
                      and (:minPrice is null or coalesce(p.salePrice, p.basePrice) >= :minPrice)
                      and (:maxPrice is null or coalesce(p.salePrice, p.basePrice) <= :maxPrice)
                    """
    )
    Page<Product> searchActiveProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("categoryPattern") String categoryPattern,
            @Param("style") String style,
            @Param("size") String size,
            @Param("color") String color,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
