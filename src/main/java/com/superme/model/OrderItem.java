package com.superme.model;

import com.superme.model.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Order Items area in popup: product image, name, size, qty, price.
 */
@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // parent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // reference to product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // snapshot fields for card + popup
    @Column(name = "product_name", nullable = false)
    private String productName;         // "Classic White Half-Sleeve Shirt"

    @Column(name = "product_thumbnail")
    private String productThumbnail;    // S3 URL

    @Column(name = "category_name")
    private String categoryName;        // Boys Uniform / Textbooks etc.

    @Column(name = "school_name")
    private String schoolName;          // snapshot

    @Column(name = "size")
    private String size;                // "Size: 8" / "Size: 32-34"

    @Column(name = "quantity", nullable = false)
    private int quantity;               // "Qty: 1"

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;       // per item selling price

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;      // qty * unitPrice
}
