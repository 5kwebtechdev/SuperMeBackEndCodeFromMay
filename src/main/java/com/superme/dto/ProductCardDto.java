package com.superme.dto;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductCardDto {

    private Long id;
    private String name;
    private String thumbnailUrl;
    private String categoryName;
    private String schoolName;
    private String tag;               // Summer / Textbook etc.
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private String discountText;
    private Boolean mandatory;
    private Boolean inStock;
}
