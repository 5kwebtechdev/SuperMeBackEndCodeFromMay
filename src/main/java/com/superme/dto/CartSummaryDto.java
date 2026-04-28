package com.superme.dto;


import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CartSummaryDto {

    private List<CartItemDto> items;
    private BigDecimal itemsTotal;
    private BigDecimal discountTotal;
    private BigDecimal payableTotal;
}
