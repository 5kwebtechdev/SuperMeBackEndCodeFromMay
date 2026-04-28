package com.superme.dto;

import com.superme.enums.FieldType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryFieldUpdateRequest {

    private FieldType fieldType;
    private String fieldKey;
    private String fieldLabel;
    private Boolean isRequired;
    private Boolean isFilterable;
    private Integer displayOrder;
    private String validationRules;
    private List<ProductFieldOptionDto> options;
}
