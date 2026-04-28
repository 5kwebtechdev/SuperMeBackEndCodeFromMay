package com.superme.dto;


import com.superme.enums.OrderStatus;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    private OrderStatus status;   // New, Packed, Shipped, Delivered, ...
    private String statusNote;    // red/green info bar text
}
