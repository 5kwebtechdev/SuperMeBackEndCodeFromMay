package com.superme.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Top-level product category: Boys Uniform, Girls Uniform, Textbooks, etc.
 * Drives filters, and dynamic fields for the Add/Edit Product form.
 */
@Entity
@Table(name = "product_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Category name exactly like Figma (e.g. "Boys Uniform")
    @Column(nullable = false, unique = true)
    private String name;

    // Optional nicer label; can be same as name
    private String displayName;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCategoryField> fields;
}
