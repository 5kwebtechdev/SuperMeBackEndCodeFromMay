package com.superme.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

/**
 * Dropdown / multi-select options for a product field.
 * Example: GST Class -> 5%, 12%, 18%.
 */
@Entity
@Table(name = "product_field_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "field_id")
    private ProductCategoryField field;

    @Column(nullable = false)
    private String value;        // internal value

    private String label;        // display label

    private Integer displayOrder;
}
