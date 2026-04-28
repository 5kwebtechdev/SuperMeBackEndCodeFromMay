package com.superme.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFieldOptionDto {
    private Long id;
    private String value;
    private String label;
    private Integer displayOrder;
}
