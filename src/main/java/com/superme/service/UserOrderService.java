package com.superme.service;

import com.superme.dto.*;
import com.superme.enums.OrderStatus;
import com.superme.model.Order;
import com.superme.model.OrderItem;
import com.superme.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserOrderService {

    private final OrderRepository orderRepository;
    // inject CurrentUserService when you have auth

    private Long getCurrentUserId() {
        return 1L;
    }

    public Page<UserOrderListItemDto> listOrders(Pageable pageable) {
        Long userId = getCurrentUserId();
        // add userId condition in repository in future; for now use findAll
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(this::toListItemDto);
    }

    public UserOrderDetailDto getOrder(Long orderId) {
        Long userId = getCurrentUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        // later: assert order.getUserId().equals(userId)
        return toDetailDto(order);
    }

    public void cancelOrder(Long orderId, UserCancelOrderRequest request) {
        Long userId = getCurrentUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!OrderStatus.NEW.equals(order.getStatus())
                && !OrderStatus.PACKED.equals(order.getStatus())) {
            throw new RuntimeException("Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setStatusNote("Cancelled by user. Reason: " + request.getReason());
        orderRepository.save(order);
    }

    public void createReturnOrExchange(Long orderId, UserReturnOrExchangeRequest request) {
        Long userId = getCurrentUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!OrderStatus.DELIVERED.equals(order.getStatus())) {
            throw new RuntimeException("Return / exchange allowed only for DELIVERED orders");
        }

        if ("Exchange".equalsIgnoreCase(request.getActionType())) {
            order.setStatus(OrderStatus.EXCHANGE_REQUESTED);
            order.setStatusNote("Exchange requested: " + request.getReason());
        } else {
            order.setStatus(OrderStatus.RETURN_INITIATED);
            order.setStatusNote("Return initiated: " + request.getReason());
        }

        orderRepository.save(order);
        // later: store per-item return/exchange records in a separate table
    }

    // ===== mapping helpers =====

    private UserOrderListItemDto toListItemDto(Order order) {
        return UserOrderListItemDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .placedAt(order.getPlacedAt())
                .schoolName(order.getSchool() != null ? order.getSchool().getSchoolName() : null)
                .itemsCount(order.getItems() != null ? order.getItems().size() : 0)
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod().name())
                .status(order.getStatus().name())
                .build();
    }

    private UserOrderDetailDto toDetailDto(Order order) {
        boolean canCancel = order.getStatus() == OrderStatus.NEW || order.getStatus() == OrderStatus.PACKED;
        boolean canReturnOrExchange = order.getStatus() == OrderStatus.DELIVERED;

        return UserOrderDetailDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .placedAt(order.getPlacedAt())
                .schoolName(order.getSchool() != null ? order.getSchool().getSchoolName() : null)
                .shippingAddress(order.getShippingAddress() != null
                        ? AddressDto.builder()
                        .id(null)
                        .receiverName(order.getCustomerName())
                        .mobile(order.getCustomerMobile())
                        .line1(order.getShippingAddress().getLine1())
                        .line2(order.getShippingAddress().getLine2())
                        .city(order.getShippingAddress().getCity())
                        .state(order.getShippingAddress().getState())
                        .postalCode(order.getShippingAddress().getPostalCode())
                        .build()
                        : null)
                .items(order.getItems() != null
                        ? order.getItems().stream().map(this::toUserOrderItemDto).toList()
                        : List.of())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentMethod().name()) // or real status field if you add one
                .status(order.getStatus().name())
                .statusNote(order.getStatusNote())
                .trackingId(order.getShipmentInfo() != null ? order.getShipmentInfo().getTrackingId() : null)
                .trackingLink(order.getShipmentInfo() != null ? order.getShipmentInfo().getTrackingLink() : null)
                .canCancel(canCancel)
                .canReturnOrExchange(canReturnOrExchange)
                .build();
    }

    private UserOrderItemDto toUserOrderItemDto(OrderItem item) {
        return UserOrderItemDto.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .thumbnailUrl(item.getProductThumbnail())
                .categoryName(item.getCategoryName())
                .sizeLabel(item.getSize())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getTotalPrice())
                .build();
    }
}
