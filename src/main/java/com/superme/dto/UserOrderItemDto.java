package com.superme.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserOrderItemDto {

    private Long id;
    private Long productId;
    private String productName;
    private String thumbnailUrl;
    private String categoryName;

    private String sizeLabel;
    private Integer quantity;

    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
