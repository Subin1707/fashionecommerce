package com.fashion.product.repository;

import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProduct(Product product);
    Optional<ProductVariant> findBySku(String sku);
    Optional<ProductVariant> findByProductAndColorIgnoreCaseAndSizeIgnoreCase(Product product, String color, String size);
    boolean existsByProductAndColorIgnoreCaseAndSizeIgnoreCase(Product product, String color, String size);
    List<ProductVariant> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(v.stockQty), 0) FROM ProductVariant v")
    Long sumStockQuantity();
}
