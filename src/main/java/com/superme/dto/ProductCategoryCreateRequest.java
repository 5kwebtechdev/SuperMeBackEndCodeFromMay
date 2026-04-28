package com.superme.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryCreateRequest {
    private String name;
    private String displayName;
}
