package com.fashion.product.repository;

import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductImage;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    interface ProductImageSummary {
        Long getProductId();
        Long getId();
        String getImageUrl();
        Boolean getIsPrimary();
        Integer getDisplayOrder();
    }

    List<ProductImage> findByProduct(Product product);
    List<ProductImage> findByProductIn(List<Product> products);
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    @Query("""
            select image.product.id as productId,
                   image.id as id,
                   image.imageUrl as imageUrl,
                   image.isPrimary as isPrimary,
                   image.displayOrder as displayOrder
            from ProductImage image
            where image.product.id in :productIds
            order by image.product.id, image.isPrimary desc, image.displayOrder, image.id
            """)
    List<ProductImageSummary> findSummariesByProductIdIn(@Param("productIds") Collection<Long> productIds);
}
