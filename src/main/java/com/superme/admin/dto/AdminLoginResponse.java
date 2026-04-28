package com.superme.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {
    private int status;
    private String token;
    private AdminDetails admin;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDetails {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private String department;
        private String designation;
        private String role;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime expiryDate;

        // Add other admin fields if needed
        private boolean isActive;
    }
}