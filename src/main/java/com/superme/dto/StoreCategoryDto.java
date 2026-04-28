package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StoreCategoryDto {
    private Long id;
    private String name;          // "Boys Uniform"
    private String iconUrl;
}
