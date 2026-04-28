package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpdateCartItemRequest {

    private String sizeLabel;
    private Integer quantity;
}

