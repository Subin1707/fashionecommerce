package com.fashion.admin;

import com.fashion.admin.dto.AdminProductDto;
import com.fashion.admin.dto.CreateProductRequest;
import com.fashion.admin.dto.UpdateProductRequest;
import com.fashion.admin.service.AdminProductService;
import com.fashion.brand.entity.Brand;
import com.fashion.brand.repository.BrandRepository;
import com.fashion.category.entity.Category;
import com.fashion.category.repository.CategoryRepository;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductSizeChartRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.search.service.ProductSearchService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductSizeChartRepository productSizeChartRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductSearchService productSearchService;

    @InjectMocks
    private AdminProductService adminProductService;

    @Test
    void testCreateProduct() {
        Brand brand = Brand.builder()
                .id(1L)
                .name("Nike")
                .slug("nike")
                .build();

        Category category = Category.builder()
                .id(1L)
                .name("T-Shirts")
                .slug("t-shirts")
                .build();

        CreateProductRequest request = CreateProductRequest.builder()
                .stockQty(10)
                .brandId(1L)
                .categoryId(1L)
                .name("Nike T-Shirt")
                .description("A nice t-shirt")
                .basePrice(BigDecimal.valueOf(100.00))
                .salePrice(BigDecimal.valueOf(80.00))
                .material("Cotton")
                .fit("Regular")
                .gender("M")
                .status("ACTIVE")
                .isFeatured(true)
                .isNew(false)
                .build();

        Product product = Product.builder()
                .id(1L)
                .brand(brand)
                .category(category)
                .name("Nike T-Shirt")
                .slug("nike-t-shirt")
                .description("A nice t-shirt")
                .basePrice(BigDecimal.valueOf(100.00))
                .salePrice(BigDecimal.valueOf(80.00))
                .material("Cotton")
                .fit("Regular")
                .gender("M")
                .status("ACTIVE")
                .isFeatured(true)
                .isNew(false)
                .build();

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(product);
        doNothing().when(productSearchService).syncProductsToElasticsearch();
        when(productVariantRepository.findByProduct(any())).thenReturn(java.util.Collections.emptyList());
        when(productImageRepository.findByProduct(any())).thenReturn(java.util.Collections.emptyList());
        when(productSizeChartRepository.findByProductOrderByIdAsc(any())).thenReturn(java.util.Collections.emptyList());

        AdminProductDto result = adminProductService.createProduct(request);

        assertNotNull(result);
        assertEquals("Nike T-Shirt", result.getName());
        assertEquals("Nike", result.getBrandName());
        verify(productVariantRepository).save(argThat(variant ->
                variant.getProduct() == product
                        && variant.getStockQty() == 10
                        && "DEFAULT".equals(variant.getSize())
                        && "DEFAULT".equals(variant.getColor())));
    }

    @Test
    void testUpdateProduct() {
        Brand brand = Brand.builder()
                .id(1L)
                .name("Nike")
                .slug("nike")
                .build();

        Category category = Category.builder()
                .id(1L)
                .name("T-Shirts")
                .slug("t-shirts")
                .build();

        Product product = Product.builder()
                .id(1L)
                .brand(brand)
                .category(category)
                .name("Old Name")
                .slug("old-name")
                .basePrice(BigDecimal.valueOf(100.00))
                .build();

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("New Name")
                .basePrice(BigDecimal.valueOf(120.00))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(product);
        doNothing().when(productSearchService).syncProductsToElasticsearch();
        when(productVariantRepository.findByProduct(any())).thenReturn(java.util.Collections.emptyList());
        when(productImageRepository.findByProduct(any())).thenReturn(java.util.Collections.emptyList());
        when(productSizeChartRepository.findByProductOrderByIdAsc(any())).thenReturn(java.util.Collections.emptyList());

        AdminProductDto result = adminProductService.updateProduct(1L, request);

        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals(BigDecimal.valueOf(120.00), result.getBasePrice());
    }
}
