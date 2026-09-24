package com.fashion.product.repository;

import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductSizeChart;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSizeChartRepository extends JpaRepository<ProductSizeChart, Long> {
    List<ProductSizeChart> findByProductOrderByIdAsc(Product product);

    void deleteByProduct(Product product);
}
