package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserReturnOrExchangeRequest {

    private Long orderItemId;
    private String actionType;    // "Return" / "Exchange"
    private String reason;
    private String exchangeSize;
    private String photoUrl;
}
