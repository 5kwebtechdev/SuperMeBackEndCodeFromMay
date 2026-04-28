package com.superme.dto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RefundOrderRequest {

    private Long orderId;
    private String refundMethod;    // e.g. "Original payment method", "Wallet"
    private Integer refundAmount;   // ₹ amount
    private String statusNote;      // green bar text
}
