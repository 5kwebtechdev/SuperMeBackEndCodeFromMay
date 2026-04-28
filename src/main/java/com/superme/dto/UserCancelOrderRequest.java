package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserCancelOrderRequest {
    private String reason;
}