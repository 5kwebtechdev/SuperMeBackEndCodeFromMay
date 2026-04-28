package com.superme.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDate;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShipmentInfo {

    // For Mark as Shipped / Schedule Reverse Pickup popup
    private String courierPartner;    // "Delhivery", "Shadowfax"
    private String trackingId;        // "AWB / Tracking ID"
    private String trackingLink;      // "https://track.courierpartner.com/..."

    private LocalDate pickupDate;     // for reverse pickup, exchange, etc.
    private LocalDate deliveryDate;   // for confirm delivery popup

    private boolean notifyCustomer;   // "Send SMS/WhatsApp update to customer"
}
