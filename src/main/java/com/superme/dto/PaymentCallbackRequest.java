package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentCallbackRequest {

    private String paymentGateway;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String gatewaySignature;
    private String status;           // SUCCESS / FAILED
}
