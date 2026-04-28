package com.superme.dto;

import com.superme.dto.CartItemDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CheckoutSummaryResponse {

    private Long childId;
    private Long addressId;
    private String paymentMethod;

    private List<CartItemDto> items;
    private BigDecimal itemsTotal;
    private BigDecimal discountTotal;
    private BigDecimal deliveryFee;
    private BigDecimal payableTotal;
}
