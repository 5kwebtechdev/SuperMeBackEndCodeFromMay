package com.superme.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.superme.enums.FieldType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Configuration for one dynamic field for a given category
 * (e.g. for Textbooks -> Author Name, Publisher Name, Edition/Year).
 */
@Entity
@Table(name = "product_category_fields",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "field_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;     // TEXT, NUMBER, SELECT, MULTI_SELECT, CHECKBOX, etc.

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;         // e.g. "authorName", "size", "gstClass"

    @Column(name = "field_label", nullable = false, length = 100)
    private String fieldLabel;       // label shown to admin

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_filterable")
    private Boolean isFilterable = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "validation_rule", columnDefinition = "JSON")
    private String validationRules;  // extra validations (min/max, regex etc.)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductFieldOption> options;
}
