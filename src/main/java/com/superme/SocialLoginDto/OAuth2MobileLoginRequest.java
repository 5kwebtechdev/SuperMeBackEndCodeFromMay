//package com.superme.SocialLoginDto;
//
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import lombok.Data;
//
//@Data
//public class OAuth2MobileLoginRequest {
//
//    @NotBlank(message = "Provider is required")
//    private String provider;  // google, github
//
//    @NotBlank(message = "Provider ID is required")
//    private String providerId;
//
//    @Email(message = "Invalid email format")
//    @NotBlank(message = "Email is required")
//    private String email;
//
//    private String name;
//
//    private String avatarUrl;
//}