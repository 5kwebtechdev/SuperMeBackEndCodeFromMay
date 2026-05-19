package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {

    private boolean success;
    private String message;

    public static OtpResponse success(String message) {
        return new OtpResponse(true, message);
    }

    public static OtpResponse failure(String message) {
        return new OtpResponse(false, message);
    }
}