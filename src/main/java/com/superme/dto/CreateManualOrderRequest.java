package com.superme.dto;

import com.superme.enums.PaymentMethod;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateManualOrderRequest {

    // From popup "Create Manual Order"
    private String customerName;
    private String customerMobile;
    private String customerEmail;

    private Long schoolId;                 // School Name*

    private ShippingAddressRequest shippingAddress;

    private PaymentMethod paymentMethod;   // Payment: Paid / COD

    private List<ManualOrderItemRequest> items;
}
