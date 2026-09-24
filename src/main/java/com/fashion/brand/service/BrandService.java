package com.fashion.brand.service;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));
    }

    @Transactional
    public Brand createBrand(Brand brand) {
        if (brandRepository.findByName(brand.getName()).isPresent()) {
            throw new RuntimeException("Brand already exists: " + brand.getName());
        }
        return brandRepository.save(brand);
    }

    @Transactional
    public Brand updateBrand(Long id, Brand brand) {
        Brand existing = getBrandById(id);
        existing.setName(brand.getName());
        existing.setSlug(brand.getSlug());
        existing.setDescription(brand.getDescription());
        existing.setLogoUrl(brand.getLogoUrl());
        existing.setIsActive(brand.getIsActive());
        return brandRepository.save(existing);
    }

    @Transactional
    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new RuntimeException("Brand not found with id: " + id);
        }
        brandRepository.deleteById(id);
    }
}
