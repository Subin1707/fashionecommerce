package com.fashion.product.service;

import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;

    @Transactional(readOnly = true)
    public List<ProductVariant> getVariantsByProduct(Product product) {
        return productVariantRepository.findByProduct(product);
    }

    @Transactional(readOnly = true)
    public List<ProductVariant> getVariantsByProductId(Long productId) {
        return productVariantRepository.findByProductId(productId);
    }

    @Transactional
    public ProductVariant createVariant(ProductVariant variant) {
        return productVariantRepository.save(variant);
    }
}
