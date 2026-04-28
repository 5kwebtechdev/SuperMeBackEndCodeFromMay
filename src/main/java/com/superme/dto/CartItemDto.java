package com.superme.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CartItemDto {

    private Long id;
    private Long productId;
    private String productName;
    private String thumbnailUrl;
    private String categoryName;
    private String schoolName;

    private String sizeLabel;
    private Integer quantity;

    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private BigDecimal lineTotal;
    private Boolean inStock;
}
