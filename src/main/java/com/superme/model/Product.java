package com.superme.model;

import com.superme.model.School;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Core Product entity used by the Student Store.
 * Matches the Add/Edit Product forms for all categories.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                     // ID in card (ID: 1, ID: 2, ...)

    // Category * (Boys Uniform, Textbooks, Footwears, Accessories, General Books, Entrance Exam Books)
    private String category;

    // Product Images * (S3 URLs)
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> productImages;

    // Product Details
    private String productName;          // Product Name *
    @Column(length = 1000)
    private String description;          // Description *
    @Column(length = 4000)
    private String productDetails;       // Product Details *
    private String productTag;           // Product Tag * (Genre / Summer etc.)

    // Category Details
    @ManyToOne
    @JoinColumn(name = "school_id")      // School Name * (FK to School entity)
    private School school;

    private boolean markAsMandatoryItem; // Mark as mandatory item
    private String className;            // Class * (for Textbooks / General Books)
    private String authorName;           // Author Name * (books)
    private String publisherName;        // Publisher Name * (books)
    private String editionYear;          // Edition / Year *
    private String ageGroup;             // Age Group *

    // Inventory & Pricing
    private double mrp;                  // MRP *
    private double sellingPrice;         // Selling Price *
    private String discountText;         // Discount text (e.g. "10% Off")
    private String gstClass;             // GST Class *

    // Stocks
    private int stock;                   // Stock * (= sum of all sizes if apparel)

    // Sizes & Stocks (for uniforms / footwears)
    private String sizeChartTemplate;    // Size Chart Template * (Apparel, Footwear (UK))

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSizeRow> sizes;  // repeated Size/MRP/Selling Price/Stock rows

    private String sizeChartInImageUrl;  // Size Chart (In) *
    private String sizeChartCmImageUrl;  // Size Chart (Cm) *

    // Status
    private String status;               // Active / Inactive *
}
