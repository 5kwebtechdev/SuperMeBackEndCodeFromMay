package com.superme.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductSizeOptionDto {

    private String sizeLabel;         // "4", "6", "UK 5"
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private Integer stock;
    private Boolean available;
}
