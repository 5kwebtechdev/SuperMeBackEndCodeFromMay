package com.superme.controller;

import com.superme.dto.OAuth2MobileLoginRequest;
import com.superme.dto.LoginResponse;
import com.superme.dto.UserDTO;
import com.superme.enums.Role;
import com.superme.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final EmailService emailService;

    @GetMapping("/test-mail")
    public String testMail() {

        emailService.sendMail(
                "harjit5kwt@gmail.com",
                "SMTP Working",
                "Spring Boot Gmail SMTP is working successfully"
        );

        return "Mail Sent";
    }
}