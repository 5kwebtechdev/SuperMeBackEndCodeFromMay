package com.superme.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetCartRequest {
    private Long userId;
    private Long schoolId;
    private List<String> className;
}

