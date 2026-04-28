package com.superme.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductDetailDto {

    private Long id;
    private String name;
    private String description;
    private String details;
    private String categoryName;
    private String schoolName;
    private String tag;
    private String ageGroup;
    private Boolean mandatory;

    private List<String> images;

    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private String discountText;
    private String gstClass;
    private Integer stock;

    private String sizeChartTemplate;
    private String sizeChartInImageUrl;
    private String sizeChartCmImageUrl;

    private List<ProductSizeOptionDto> sizes;
}
