package com.superme.dto;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItemDto {

    private Long id;
    private Long productId;

    private String productName;
    private String productThumbnail;
    private String categoryName;
    private String schoolName;

    private String size;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
