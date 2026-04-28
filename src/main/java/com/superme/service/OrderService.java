package com.superme.service;

import com.superme.dto.*;
import com.superme.enums.OrderStatus;
import com.superme.model.Order;
import com.superme.model.OrderItem;
import com.superme.model.Product;
import com.superme.repository.OrderRepository;
import com.superme.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository  productRepository;

    public Page<OrderListItemDto> listOrders(OrderStatus status, String search, Pageable pageable) {
        Page<Order> orders;
        if (search != null && !search.trim().isEmpty()) {
            orders = orderRepository
                    .findByOrderNumberContainingIgnoreCaseOrCustomerNameContainingIgnoreCaseOrCustomerMobileContaining(
                            search, search, search, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(this::mapToListItem);
    }

    public OrderDetailDto getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        return mapToDetail(order);
    }

    public Long createManualOrder(CreateManualOrderRequest request) {
        log.info("Creating manual order for customer: {}", request.getCustomerName());

        com.superme.model.ShippingAddress shippingAddress = null;
        if (request.getShippingAddress() != null) {
            shippingAddress = com.superme.model.ShippingAddress.builder()
                    .line1(request.getShippingAddress().getLine1())
                    .line2(request.getShippingAddress().getLine2())
                    .city(request.getShippingAddress().getCity())
                    .state(request.getShippingAddress().getState())
                    .postalCode(request.getShippingAddress().getPostalCode())
                    .build();
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerName(request.getCustomerName())
                .customerMobile(request.getCustomerMobile())
                .customerEmail(request.getCustomerEmail())
                .shippingAddress(shippingAddress)
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.NEW)
                .manualOrder(true)
                .totalAmount(calculateTotal(request.getItems()))
                .build();

        List<OrderItem> orderItems = request.getItems().stream()
                .map(reqItem -> mapToOrderItem(order, reqItem))
                .collect(Collectors.toList());
        order.setItems(orderItems);

        orderRepository.save(order);
        return order.getId();
    }


    public void updateStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = getOrderById(id);
        order.setStatus(request.getStatus());
        order.setStatusNote(request.getStatusNote());
        orderRepository.save(order);
    }








    public void acceptAndPack(Long id, AcceptAndPackRequest request) {
        Order order = getOrderById(id);
        validateStatus(order, OrderStatus.NEW);

        order.setStatus(OrderStatus.PACKED);
        order.setPackedAt(LocalDateTime.now());
        order.setStatusNote(request.getConfirmationNote());
        orderRepository.save(order);
    }










    public void markAsShipped(Long id, MarkAsShippedRequest request) {
        Order order = getOrderById(id);
        validateStatus(order, OrderStatus.PACKED);

        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());

        // ShipmentInfo is @Embedded inside Order
        if (order.getShipmentInfo() == null) {
            order.setShipmentInfo(new com.superme.model.ShipmentInfo());
        }
        order.getShipmentInfo().setCourierPartner(request.getCourierPartner());
        order.getShipmentInfo().setTrackingId(request.getTrackingId());
        order.getShipmentInfo().setTrackingLink(request.getTrackingLink());

        if (Boolean.TRUE.equals(request.getNotifyCustomer())) {
            order.setStatusNote("Shipped via " + request.getCourierPartner());
        }
        orderRepository.save(order);
    }

    public void markAsDelivered(Long id, MarkAsDeliveredRequest request) {
        Order order = getOrderById(id);
        validateStatus(order, OrderStatus.SHIPPED);

        LocalDate deliveryDate = LocalDate.parse(request.getDeliveryDate());
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(deliveryDate.atStartOfDay());
        order.setStatusNote(request.getStatusNote());
        orderRepository.save(order);
    }

    public void initiateReturn(Long id, InitiateReturnRequest request) {
        Order order = getOrderById(id);
        validateStatus(order, OrderStatus.DELIVERED);

        order.setStatus(OrderStatus.RETURN_INITIATED);
        order.setReturnInitiatedAt(LocalDateTime.now());
        order.setStatusNote("Return initiated: " + request.getReason());
        // if you store reason/photo on ShipmentInfo or another entity, set there
        orderRepository.save(order);
    }

    public void markReturnReceived(Long id, ReturnReceivedChecklistRequest request) {
        Order order = getOrderById(id);
        validateStatus(order, OrderStatus.RETURN_INITIATED);

        order.setStatus(OrderStatus.RETURN_RECEIVED);
        order.setReturnReceivedAt(LocalDateTime.now());
        order.setStatusNote(request.getStatusNote());
        orderRepository.save(order);
    }

    public void refundOrder(Long id, RefundOrderRequest request) {
        Order order = getOrderById(id);
        validateStatus(order, OrderStatus.RETURN_RECEIVED);

        order.setStatus(OrderStatus.REFUNDED);
        // if you add refund fields later (amount, timestamp) put here
        order.setStatusNote(request.getStatusNote());
        orderRepository.save(order);
    }

    public void acceptExchangeRequest(Long id, AcceptExchangeRequest request) {
        Order order = getOrderById(id);
        validateStatus(order, OrderStatus.RETURN_INITIATED);

        order.setStatus(OrderStatus.EXCHANGE_REQUESTED);
        order.setStatusNote(request.getStatusNote());
        orderRepository.save(order);
    }

    public void shipExchange(Long id, ShipExchangeRequest request) {
        Order order = getOrderById(id);
        validateStatus(order, OrderStatus.EXCHANGE_REQUESTED);

        order.setStatus(OrderStatus.EXCHANGE_SHIPPED);
        order.setExchangeShippedAt(LocalDateTime.now());

        if (order.getShipmentInfo() == null) {
            order.setShipmentInfo(new com.superme.model.ShipmentInfo());
        }
        order.getShipmentInfo().setCourierPartner(request.getCourierPartner());
        order.getShipmentInfo().setTrackingId(request.getTrackingId());
        order.getShipmentInfo().setTrackingLink(request.getTrackingLink());

        order.setStatusNote(request.getStatusNote());
        orderRepository.save(order);
    }

    public void cancelOrder(Long id, CancelOrderRequest request) {
        Order order = getOrderById(id);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setStatusNote(request.getStatusNote());
        orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found: " + id);
        }
        orderRepository.deleteById(id);
    }

    // ================== helpers ==================

    private Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    private void validateStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new RuntimeException(
                    "Order must be " + expected + " but is " + order.getStatus());
        }
    }

    private OrderListItemDto mapToListItem(Order order) {
        return OrderListItemDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .placedAt(order.getPlacedAt())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .schoolName(order.getSchool() != null ? order.getSchool().getSchoolName() : null)
                .itemsCount(order.getItems() != null ? order.getItems().size() : 0)
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .build();
    }

    private OrderDetailDto mapToDetail(Order order) {
        return OrderDetailDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .placedAt(order.getPlacedAt())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerEmail(order.getCustomerEmail())
                .schoolName(order.getSchool() != null ? order.getSchool().getSchoolName() : null)
                .shippingAddress(order.getShippingAddress())
                .items(order.getItems() != null
                        ? order.getItems().stream().map(this::mapToOrderItemDto).toList()
                        : List.of())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentReference(order.getPaymentReference())
                .status(order.getStatus())
                .statusNote(order.getStatusNote())
                .shipmentInfo(order.getShipmentInfo())
                .packedAt(order.getPackedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .returnInitiatedAt(order.getReturnInitiatedAt())
                .returnReceivedAt(order.getReturnReceivedAt())
                .exchangeShippedAt(order.getExchangeShippedAt())
                .cancelledAt(order.getCancelledAt())
                .build();
    }

//    private OrderItem mapToOrderItem(Order order, ManualOrderItemRequest req) {
//        return OrderItem.builder()
//                .order(order)
//                .product(null) // load product and set if needed
//                .productName(null) // or from product
//                .productThumbnail(null)
//                .categoryName(null)
//                .schoolName(order.getSchool() != null ? order.getSchool().getSchoolName() : null)
//                .size(req.getSize())
//                .quantity(req.getQuantity())
//                .unitPrice(req.getUnitPrice())
//                .totalPrice(req.getUnitPrice().multiply(BigDecimal.valueOf(req.getQuantity())))
//                .build();
//    }



    private OrderItem mapToOrderItem(Order order, ManualOrderItemRequest req) {

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with id: " + req.getProductId()
                ));

        return OrderItem.builder()
                .order(order)

                // ✅ VERY IMPORTANT (this sets product_id in DB)
                .product(product)

                // ✅ Store snapshot data (best practice)
                .productName(product.getProductName())
                .productThumbnail(
                        product.getProductImages() != null && !product.getProductImages().isEmpty()
                                ? product.getProductImages().get(0)
                                : null
                )
                .categoryName(product.getCategory())
                .schoolName(product.getSchool() != null ? product.getSchool().getSchoolName() : null)

                .size(req.getSize())
                .quantity(req.getQuantity())
                .unitPrice(req.getUnitPrice())
                .totalPrice(
                        req.getUnitPrice().multiply(BigDecimal.valueOf(req.getQuantity()))
                )
                .build();
    }

    private OrderItemDto mapToOrderItemDto(OrderItem item) {
        return OrderItemDto.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .productThumbnail(item.getProductThumbnail())
                .categoryName(item.getCategoryName())
                .schoolName(item.getSchoolName())
                .size(item.getSize())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    private BigDecimal calculateTotal(List<ManualOrderItemRequest> items) {
        return items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }
}
