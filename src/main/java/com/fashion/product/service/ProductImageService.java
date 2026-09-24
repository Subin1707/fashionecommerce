package com.fashion.product.service;

import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductImage;
import com.fashion.product.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;

    @Transactional(readOnly = true)
    public List<ProductImage> getImagesByProduct(Product product) {
        return productImageRepository.findByProduct(product);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
    }

    @Transactional
    public ProductImage createImage(ProductImage productImage) {
        return productImageRepository.save(productImage);
    }
}
