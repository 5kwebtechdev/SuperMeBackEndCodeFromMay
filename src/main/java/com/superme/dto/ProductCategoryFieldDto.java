package com.superme.dto;

import com.superme.enums.FieldType;
import lombok.*;

import java.util.List;

/**
 * Returned to frontend so it knows what inputs to render
 * when a category is selected in Add/Edit Product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryFieldDto {

    private Long id;
    private Long categoryId;
    private FieldType fieldType;
    private String fieldKey;
    private String fieldLabel;
    private Boolean isRequired;
    private Boolean isFilterable;
    private Integer displayOrder;
    private String validationRules;
    private List<ProductFieldOptionDto> options;
}
