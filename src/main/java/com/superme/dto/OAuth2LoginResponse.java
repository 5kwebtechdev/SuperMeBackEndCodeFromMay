package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2LoginResponse {
    private String token;
    private Long userId;
    private String email;
    private String name;
    private boolean isNewUser;
}