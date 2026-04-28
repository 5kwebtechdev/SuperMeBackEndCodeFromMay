package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AddToCartRequest {
    private Long schoolId;
    private Long classId;
    private Long productId;
    private String sizeLabel;
    private Integer quantity;
    private Integer userId;
}
