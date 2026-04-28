package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SaveAddressRequest {

    private Long userId;
    private Long id;
    private String receiverName;
    private String mobile;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private Double latitude;
    private Double longitude;
}
