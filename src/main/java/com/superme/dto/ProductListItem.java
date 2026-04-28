package com.superme.dto;

import lombok.*;

/**
 * Data for one card in Products grid.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListItem {

    private Long id;
    private String category;
    private String schoolName;
    private String productName;
    private String description;
    private double mrp;
    private double sellingPrice;
    private int stock;
    private String status;
    private String productTag;
    private String thumbnailUrl;
}
