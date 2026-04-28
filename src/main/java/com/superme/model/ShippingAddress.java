package com.superme.model;


import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShippingAddress {
    // Customer Address * (Create Manual Order popup)
    private String line1;         // "123 Main Street, Apartment 4B"
    private String line2;         // optional
    private String city;          // "Mumbai"
    private String state;         // "Maharashtra"
    private String postalCode;    // "110001"
}
