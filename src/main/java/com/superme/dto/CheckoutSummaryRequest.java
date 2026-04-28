package com.superme.dto;


import com.superme.dto.CartItemDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CheckoutSummaryRequest {

    private Long childId;
    private Long addressId;
    private String paymentMethod;   // "ONLINE" / "COD"
}
