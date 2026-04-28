package com.superme.dto;

import com.superme.enums.OrderStatus;
import com.superme.enums.PaymentMethod;
import com.superme.model.ShipmentInfo;
import com.superme.model.ShippingAddress;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderDetailDto {

    private Long id;
    private String orderNumber;
    private LocalDateTime placedAt;

    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private String schoolName;

    private ShippingAddress shippingAddress;

    private List<OrderItemDto> items;

    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private String paymentReference;

    private OrderStatus status;
    private String statusNote;

    private ShipmentInfo shipmentInfo;

    private LocalDateTime packedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime returnInitiatedAt;
    private LocalDateTime returnReceivedAt;
    private LocalDateTime exchangeShippedAt;
    private LocalDateTime cancelledAt;
}
