package com.superme.controller;

import com.superme.dto.AccountDeleteRequestDTO;
import com.superme.dto.AccountDeleteResponseDTO;
import com.superme.service.AccountDeletionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountDeletionController {

    @Autowired
    private AccountDeletionService accountDeletionService;

    @PostMapping("/delete-request")
    public ResponseEntity<AccountDeleteResponseDTO> requestAccountDeletion(
            @Valid @RequestBody AccountDeleteRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {

        AccountDeleteResponseDTO response = accountDeletionService.processDeleteRequest(request, authHeader);
        return ResponseEntity.ok(response);
    }
}