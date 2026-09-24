package com.fashion.search.repository;

import com.fashion.search.document.ProductDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    Page<ProductDocument> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(
            String name, String description, Pageable pageable);
    
    Page<ProductDocument> findByStatus(String status, Pageable pageable);

    List<ProductDocument> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(String name, String description);

    List<ProductDocument> findByStatus(String status);
    
    Page<ProductDocument> findByBrandName(String brandName, Pageable pageable);
    
    Page<ProductDocument> findByCategoryName(String categoryName, Pageable pageable);
    
    List<ProductDocument> findByStatusOrderByAverageRatingDesc(String status);
}
