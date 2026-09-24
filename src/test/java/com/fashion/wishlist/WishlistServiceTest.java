package com.fashion.wishlist;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.category.entity.Category;
import com.fashion.category.service.CategoryService;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.wishlist.entity.WishlistItem;
import com.fashion.wishlist.service.WishlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WishlistServiceTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void shouldAddAndRemoveProductFromWishlist() {
        Brand brand = brandService.createBrand(Brand.builder()
            .name("WishlistTestBrand")
            .slug("wishlist-test-brand")
            .description("Brand for wishlist test")
            .build());
        Category category = categoryService.createCategory(Category.builder()
            .name("WishlistTestCategory")
            .slug("wishlist-test-category")
            .description("Category for wishlist test")
            .build());

        Product product = productRepository.save(Product.builder()
                .name("Áo thun basic")
                .slug("ao-thun-basic")
                .description("Mô tả")
                .basePrice(java.math.BigDecimal.valueOf(200000.0))
                .salePrice(java.math.BigDecimal.valueOf(180000.0))
                .category(category)
                .brand(brand)
                .material("Cotton")
                .status("ACTIVE")
                .build());

        WishlistItem saved = wishlistService.addToWishlist(12L, product.getId());
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(wishlistService.getWishlist(12L)).hasSize(1);

        wishlistService.removeFromWishlist(12L, product.getId());
        assertThat(wishlistService.getWishlist(12L)).isEmpty();
    }
}
