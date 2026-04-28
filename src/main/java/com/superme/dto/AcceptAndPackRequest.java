package com.superme.dto;

import lombok.*;

@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcceptAndPackRequest {
    // e.g. "Once accepted, this order cannot be cancelled or edited..."
    private String confirmationNote;
}