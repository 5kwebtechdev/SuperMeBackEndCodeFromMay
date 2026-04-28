package com.superme.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFormRequest {

    // Category *
    private String category;

    // Product Details
    private List<String> productImages;
    private String productName;
    private String description;
    private String productDetails;
    private String productTag;

    // Category Details
    private Long schoolId;
    private boolean markAsMandatoryItem;
    private String className;
    private String authorName;
    private String publisherName;
    private String editionYear;
    private String ageGroup;

    // Inventory & Pricing
    private double mrp;
    private double sellingPrice;
    private String discountText;
    private String gstClass;

    // Stocks
    private int stock;

    // Sizes & Stocks
    private String sizeChartTemplate;
    private List<SizeRowInput> sizes;
    private String sizeChartInImageUrl;
    private String sizeChartCmImageUrl;

    // Status
    private String status;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SizeRowInput {
        private Long id;
        private String size;
        private double mrp;
        private double sellingPrice;
        private int stock;
    }
}
