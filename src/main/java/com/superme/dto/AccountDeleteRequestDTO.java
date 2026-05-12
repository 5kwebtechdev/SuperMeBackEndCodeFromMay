package com.superme.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeleteRequestDTO {
    private Long user_id;
    private String email;
    private String reason;
}

