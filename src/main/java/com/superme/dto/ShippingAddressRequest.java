package com.superme.dto;


import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShippingAddressRequest {
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
}
