package com.superme.dto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CancelOrderRequest {

    private String cancelledBy;      // "Admin", "Customer"
    private String reason;           // text from popup
    private String statusNote;       // "Cancelled on Dec 15, 11:30 AM by Rahul S."
}