package com.superme.dto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InitiateReturnRequest {

    private Long orderItemId;       // which line is being returned
    private String actionType;      // "Return" or "Exchange"
    private String reason;          // dropdown: "Size issue", "Defective", etc.
    private String exchangeSize;    // only when actionType = Exchange
    private String photoUrl;        // optional uploaded photo
}