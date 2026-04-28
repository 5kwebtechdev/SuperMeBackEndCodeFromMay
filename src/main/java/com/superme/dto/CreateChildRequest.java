package com.superme.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateChildRequest {

    private String childName;
    private Long schoolId;
    private Long classId;
}
