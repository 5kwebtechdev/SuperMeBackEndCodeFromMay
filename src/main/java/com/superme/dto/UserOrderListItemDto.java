package com.superme.dto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserOrderListItemDto {

    private Long id;
    private String orderNumber;
    private LocalDateTime placedAt;

    private String schoolName;
    private Integer itemsCount;
    private BigDecimal totalAmount;

    private String paymentMethod;
    private String status;
}
