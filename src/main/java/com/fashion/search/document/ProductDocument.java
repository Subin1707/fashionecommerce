package com.fashion.search.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@Document(indexName = "products", createIndex = false)
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Double)
    private BigDecimal basePrice;

    @Field(type = FieldType.Double)
    private BigDecimal salePrice;

    @Field(type = FieldType.Keyword)
    private String material;

    @Field(type = FieldType.Keyword)
    private String fit;

    @Field(type = FieldType.Keyword)
    private String gender;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String primaryImageUrl;

    @Field(type = FieldType.Keyword)
    private List<String> colors;

    @Field(type = FieldType.Keyword)
    private List<String> sizes;

    @Field(type = FieldType.Integer)
    private Integer totalStock;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Dense_Vector, dims = 512, similarity = "cosine")
    private List<Float> embedding;

    @Field(type = FieldType.Integer)
    private Integer totalReviews;

    @Field(type = FieldType.Boolean)
    private Boolean isFeatured;

    @Field(type = FieldType.Boolean)
    private Boolean isNew;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    public ProductDocument() {
    }
    public ProductDocument(final Long id, final String name, final String description, final String slug, final BigDecimal basePrice, final BigDecimal salePrice, final String material, final String fit, final String gender, final String status, final String brandName, final String categoryName, final String primaryImageUrl, final List<String> colors, final List<String> sizes, final Integer totalStock, final Double averageRating, final List<Float> embedding, final Integer totalReviews, final Boolean isFeatured, final Boolean isNew, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.slug = slug;
        this.basePrice = basePrice;
        this.salePrice = salePrice;
        this.material = material;
        this.fit = fit;
        this.gender = gender;
        this.status = status;
        this.brandName = brandName;
        this.categoryName = categoryName;
        this.primaryImageUrl = primaryImageUrl;
        this.colors = colors;
        this.sizes = sizes;
        this.totalStock = totalStock;
        this.averageRating = averageRating;
        this.embedding = embedding;
        this.totalReviews = totalReviews;
        this.isFeatured = isFeatured;
        this.isNew = isNew;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
