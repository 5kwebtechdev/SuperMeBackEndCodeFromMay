package com.superme.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryUpdateRequest {
    private String name;
    private String displayName;
}
