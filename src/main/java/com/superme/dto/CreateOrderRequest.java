package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateOrderRequest {

    private Long childId;
    private Long addressId;
    private String paymentMethod;   // "ONLINE" / "COD"
}
