package com.superme.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StoreHomeResponse {

    private List<StoreCategoryDto> categories;
    private List<ProductCardDto> featured;
    private List<ProductCardDto> recommended;
}
