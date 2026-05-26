package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContentBlock {
    private String type;
    private String content;
}