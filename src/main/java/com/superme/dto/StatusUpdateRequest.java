package com.superme.dto;


import com.superme.enums.Status;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    private Long Id;
    private Status status;
}
