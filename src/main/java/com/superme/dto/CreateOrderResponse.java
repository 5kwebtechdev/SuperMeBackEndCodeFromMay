package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateOrderResponse {

    private Long orderId;
    private String orderNumber;
    private String paymentMethod;
    private String paymentStatus;    // PENDING / PAID

    private String paymentGateway;   // e.g. Razorpay
    private String paymentOrderId;
    private String paymentPayload;
}
