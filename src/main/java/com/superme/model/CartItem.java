package com.superme.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // scope: user + school + class
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long schoolId;

    @Column(nullable = false)
    private Long classId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String sizeLabel;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private java.math.BigDecimal unitPrice;

    @Column(nullable = false)
    private java.math.BigDecimal lineTotal;
}
