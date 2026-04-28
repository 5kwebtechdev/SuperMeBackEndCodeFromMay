package com.superme.model;

import com.superme.enums.OrderStatus;
import com.superme.enums.PaymentMethod;
import com.superme.model.School;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Management table + all popup headers.
 */
@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                      // ORDER ID in table

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;           // e.g. "1" or "ORD-0001"

    @CreationTimestamp
    @Column(name = "placed_at", nullable = false, updatable = false)
    private LocalDateTime placedAt;       // "Dec 10, 10:30 AM"

    // CUSTOMER (table + popup)
    @Column(name = "customer_name", nullable = false)
    private String customerName;          // "Rahul S."

    @Column(name = "customer_mobile", nullable = false)
    private String customerMobile;        // "91 9876543210"

    @Column(name = "customer_email")
    private String customerEmail;

    // SCHOOL (table)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;                // "St. Xavier's High School"

    // SHIPPING ADDRESS (popup)
    @Embedded
    private ShippingAddress shippingAddress;

    // ITEMS
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;        // "2 items" etc.

    // TOTAL & PAYMENT
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;       // "₹850"

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;  // "Paid" / "COD" / "Refunded"

    @Column(name = "payment_reference")
    private String paymentReference;      // txn id etc.

    // STATUS & FLOW
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;           // dropdown: New, Packed, ...

    @Column(name = "status_note")
    private String statusNote;            // red / green banner messages

    // SHIPPING / COURIER (Mark as Shipped, Reverse Pickup, etc.)
    @Embedded
    private ShipmentInfo shipmentInfo;

    // FLAGS
    @Column(name = "is_manual_order")
    private boolean manualOrder;          // created via "Create Manual Order"

    // AUDIT
    @Column(name = "packed_at")
    private LocalDateTime packedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "return_initiated_at")
    private LocalDateTime returnInitiatedAt;

    @Column(name = "return_received_at")
    private LocalDateTime returnReceivedAt;

    @Column(name = "exchange_shipped_at")
    private LocalDateTime exchangeShippedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
