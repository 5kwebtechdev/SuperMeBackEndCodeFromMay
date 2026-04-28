package com.superme.dto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AcceptExchangeRequest {

    private Long orderItemId;
    private String exchangeSize;    // "Size 32"
    private String statusNote;      // red/green info text at top
}