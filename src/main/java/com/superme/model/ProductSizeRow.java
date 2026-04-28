package com.superme.model;
import jakarta.persistence.*;
import lombok.*;

/**
 * One row in "Sizes & Stocks" section for apparel/footwear categories.
 */
@Entity
@Table(name = "product_sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSizeRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String size;         // Size *
    private double mrp;          // MRP *
    private double sellingPrice; // Selling Price *
    private int stock;           // Stock *

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
