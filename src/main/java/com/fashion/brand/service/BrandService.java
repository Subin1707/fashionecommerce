package com.fashion.brand.service;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandRepository brandRepository;

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
        validateBrand(brand, null);
        brand.setName(brand.getName().trim());
        brand.setSlug(normalizeSlug(brand.getSlug(), brand.getName()));
        if (brand.getIsActive() == null) brand.setIsActive(true);
        return brandRepository.save(brand);
    }

    @Transactional
    public Brand updateBrand(Long id, Brand brand) {
        Brand existing = getBrandById(id);
        validateBrand(brand, id);
        String name = brand.getName().trim();
        String slug = normalizeSlug(brand.getSlug(), name);
        brandRepository.findByName(name).filter(match -> !match.getId().equals(id))
                .ifPresent(match -> { throw new IllegalArgumentException("Tên thương hiệu đã tồn tại"); });
        brandRepository.findBySlug(slug).filter(match -> !match.getId().equals(id))
                .ifPresent(match -> { throw new IllegalArgumentException("Slug thương hiệu đã tồn tại"); });
        existing.setName(name);
        existing.setSlug(slug);
        existing.setDescription(brand.getDescription());
        existing.setLogoUrl(brand.getLogoUrl());
        existing.setIsActive(brand.getIsActive());
        return brandRepository.save(existing);
    }

    private void validateBrand(Brand brand, Long excludedId) {
        if (brand == null || brand.getName() == null || brand.getName().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên thương hiệu");
        }
        String name = brand.getName().trim();
        if (name.length() > 150) throw new IllegalArgumentException("Tên thương hiệu tối đa 150 ký tự");
        if (brand.getDescription() != null && brand.getDescription().length() > 1000) {
            throw new IllegalArgumentException("Mô tả thương hiệu tối đa 1000 ký tự");
        }
        if (brand.getLogoUrl() != null && brand.getLogoUrl().length() > 500) {
            throw new IllegalArgumentException("Logo URL tối đa 500 ký tự");
        }
        String slug = normalizeSlug(brand.getSlug(), name);
        if (brandRepository.findByName(name).filter(match -> !match.getId().equals(excludedId)).isPresent()) {
            throw new IllegalArgumentException("Tên thương hiệu đã tồn tại: " + name);
        }
        if (brandRepository.findBySlug(slug).filter(match -> !match.getId().equals(excludedId)).isPresent()) {
            throw new IllegalArgumentException("Slug thương hiệu đã tồn tại: " + slug);
        }
    }

    private String normalizeSlug(String slug, String name) {
        String value = slug == null || slug.isBlank() ? name : slug;
        value = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(java.util.Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (value.isBlank()) throw new IllegalArgumentException("Slug thương hiệu không hợp lệ");
        if (value.length() > 150) throw new IllegalArgumentException("Slug thương hiệu tối đa 150 ký tự");
        return value;
    }

    @Transactional
    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new RuntimeException("Brand not found with id: " + id);
        }
        brandRepository.deleteById(id);
    }

}
