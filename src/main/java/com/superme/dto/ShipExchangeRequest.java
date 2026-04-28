package com.superme.dto;
// Schedule Reverse Pickup + Ship Exchange
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShipExchangeRequest {

    private Long orderItemId;

    private String courierPartner;   // Delhivery, Shadowfax
    private String pickupDate;       // yyyy-MM-dd (calendar date)
    private String pickupTimeSlot;   // textual, if UI has it

    private String trackingId;
    private String trackingLink;
    private Boolean notifyCustomer;  // Send SMS/WhatsApp

    private String statusNote;
}
