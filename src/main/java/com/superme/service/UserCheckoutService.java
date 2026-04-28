package com.superme.service;

import com.superme.dto.*;
import com.superme.model.Order;
import com.superme.model.CartItem;
import com.superme.repository.CartItemRepository;
import com.superme.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserCheckoutService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    // inject UserAddressRepository, PaymentGatewayService etc. when ready

    // mock current user
    private Long getCurrentUserId() {
        return 1L;
    }

    public CheckoutSummaryResponse buildSummary(CheckoutSummaryRequest request) {
        Long userId = getCurrentUserId();

        // TODO: derive schoolId / classId from childId; for now use dummy
        Long schoolId = 1L;
        Long classId = 1L;

        List<CartItem> cartItems =
                cartItemRepository.findByUserIdAndSchoolIdAndClassId(userId, schoolId, classId);

        List<CartItemDto> itemDtos = cartItems.stream()
                .map(this::toCartItemDto)
                .toList();

        BigDecimal itemsTotal = cartItems.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal payableTotal = itemsTotal.subtract(discountTotal);

        return CheckoutSummaryResponse.builder()
                .items(itemDtos)
                .itemsTotal(itemsTotal)
                .discountTotal(discountTotal)
                .deliveryFee(BigDecimal.ZERO)
                .payableTotal(payableTotal)
                .paymentMethod(request.getPaymentMethod())
                .addressId(request.getAddressId())
                .build();
    }

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        Long userId = getCurrentUserId();

        // TODO: derive schoolId / classId from childId as above
        Long schoolId = 1L;
        Long classId = 1L;

        List<CartItem> cartItems =
                cartItemRepository.findByUserIdAndSchoolIdAndClassId(userId, schoolId, classId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        BigDecimal itemsTotal = cartItems.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal payableTotal = itemsTotal.subtract(discountTotal);

        Order order = Order.builder()
                .orderNumber("ORD-" + System.currentTimeMillis())
                .customerName("User " + userId) // TODO from user profile
                .customerMobile("N/A")
                .customerEmail(null)
                .totalAmount(payableTotal)
                .paymentMethod(
                        "COD".equalsIgnoreCase(request.getPaymentMethod())
                                ? com.superme.enums.PaymentMethod.COD
                                : com.superme.enums.PaymentMethod.PAID
                )
                .status(com.superme.enums.OrderStatus.NEW)
                .manualOrder(false)
                .placedAt(LocalDateTime.now())
                .build();

        // you can map CartItem -> OrderItem here if you want to persist line items

        orderRepository.save(order);

        // TODO: create payment order with gateway and return payload
        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("PENDING")
                .paymentGateway("Razorpay")
                .paymentOrderId("razorpay_order_dummy")
                .paymentPayload("{\"amount\":" + payableTotal.multiply(BigDecimal.valueOf(100)).intValue() + "}")
                .build();
    }

    public void handlePaymentCallback(PaymentCallbackRequest request) {
        log.info("Handling payment callback: gatewayOrderId={}, status={}",
                request.getGatewayOrderId(), request.getStatus());

        // TODO: find order by paymentOrderId/paymentReference and update status
        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            // mark order paid
        } else {
            // mark payment failed
        }
    }

    // ===== helpers =====

    private CartItemDto toCartItemDto(CartItem item) {
        return CartItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(null)
                .thumbnailUrl(null)
                .categoryName(null)
                .schoolName(null)
                .sizeLabel(item.getSizeLabel())
                .quantity(item.getQuantity())
                .mrp(item.getUnitPrice())
                .sellingPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .inStock(true)
                .build();
    }
}
