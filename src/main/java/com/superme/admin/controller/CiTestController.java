package com.superme.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
// temp commit dfdfd dfdfd
@RestController
@RequestMapping("/admin")
public class CiTestController {

    @GetMapping("/ci-test")
    public ResponseEntity<Map<String, Object>> ciTest() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "CI/CD pipeline is working! Auto-deploy successful.",
                "deployedAt", LocalDateTime.now().toString()
        ));
    }
}