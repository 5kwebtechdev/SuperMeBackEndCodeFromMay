package com.superme.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ManualOrderItemRequest {

    private Long productId;
    private String size;
    private int quantity;
    private BigDecimal unitPrice;
}
