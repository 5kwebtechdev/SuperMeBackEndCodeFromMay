package com.superme.dto;

import com.superme.dto.AddressDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserOrderDetailDto {

    private Long id;
    private String orderNumber;
    private LocalDateTime placedAt;
    private String schoolName;

    private AddressDto shippingAddress;
    private List<UserOrderItemDto> items;

    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;

    private String status;
    private String statusNote;

    private String trackingId;
    private String trackingLink;

    private Boolean canCancel;
    private Boolean canReturnOrExchange;
}
