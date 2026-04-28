package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MarkAsShippedRequest {
    private String courierPartner;  // Delhivery, Shadowfax
    private String trackingId;      // Tracking ID / AWB
    private String trackingLink;    // Tracking Link
    private Boolean notifyCustomer; // Send SMS/WhatsApp update to customer
}
