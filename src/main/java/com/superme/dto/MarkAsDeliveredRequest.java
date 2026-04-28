package com.superme.dto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MarkAsDeliveredRequest {
    private String deliveryDate;    // "2025-12-13"
    private String statusNote;      // "Delivered on Dec 13"
}