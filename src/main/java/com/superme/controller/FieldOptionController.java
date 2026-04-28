package com.superme.controller;

import com.superme.model.FieldOption;
import com.superme.service.FieldOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tutors/fields")
@RequiredArgsConstructor
@Slf4j
public class FieldOptionController {

    private final FieldOptionService optionService;

    @GetMapping("/{fieldId}/options")
    public ResponseEntity<List<FieldOption>> getOptionsByField(@PathVariable Long fieldId) {
        log.info("GET /api/v1/fields/{}/options", fieldId);
        return ResponseEntity.ok(optionService.getOptionsByField(fieldId));
    }

    @GetMapping("/options/{optionId}")
    public ResponseEntity<FieldOption> getOptionById(@PathVariable Long optionId) {
        log.info("GET /api/v1/fields/options/{}", optionId);
        return ResponseEntity.ok(optionService.getOptionById(optionId));
    }

    @PostMapping("/{fieldId}/options")
    public ResponseEntity<FieldOption> createOption(@PathVariable Long fieldId, @RequestBody FieldOption option) {
        log.info("POST /api/v1/fields/{}/options - Creating: {}", fieldId, option.getOptionValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(optionService.createOption(fieldId, option));
    }

    @PostMapping("/{fieldId}/options/bulk")
    public ResponseEntity<List<FieldOption>> bulkCreateOptions(@PathVariable Long fieldId, @RequestBody List<FieldOption> options) {
        log.info("POST /api/v1/fields/{}/options/bulk - Creating {} options", fieldId, options.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(optionService.bulkCreateOptions(fieldId, options));
    }

    @PutMapping("/options/{optionId}")
    public ResponseEntity<FieldOption> updateOption(@PathVariable Long optionId, @RequestBody FieldOption optionDetails) {
        log.info("PUT /api/v1/fields/options/{}", optionId);
        return ResponseEntity.ok(optionService.updateOption(optionId, optionDetails));
    }

    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long optionId) {
        log.info("DELETE /api/v1/fields/options/{}", optionId);
        optionService.deleteOption(optionId);
        return ResponseEntity.noContent().build();
    }
}
