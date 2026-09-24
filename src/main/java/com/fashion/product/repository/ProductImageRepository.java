package com.fashion.product.repository;

import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProduct(Product product);
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);
}
