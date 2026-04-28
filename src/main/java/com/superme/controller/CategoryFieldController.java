package com.superme.controller;

import com.superme.model.CategoryField;
import com.superme.service.CategoryFieldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tutors/category-fields")
@RequiredArgsConstructor
@Slf4j
public class CategoryFieldController {

    private final CategoryFieldService fieldService;

    @GetMapping("/{categoryId}/fields")
    public ResponseEntity<List<CategoryField>> getFieldsByCategory(@PathVariable Long categoryId) {
        log.info("GET /tutors/category-fields/{}/fields", categoryId);
        return ResponseEntity.ok(fieldService.getFieldsByCategory(categoryId));
    }

    @GetMapping("/{fieldId}")
    public ResponseEntity<CategoryField> getFieldById(@PathVariable Long fieldId) {
        log.info("GET /tutors/category-fields/{}", fieldId);
        return ResponseEntity.ok(fieldService.getFieldById(fieldId));
    }

    @PostMapping("/{categoryId}/fields")
    public ResponseEntity<CategoryField> createField(@PathVariable Long categoryId, @RequestBody CategoryField field) {
        log.info("POST /tutors/category-fields/{}/fields - Creating: {}", categoryId, field.getFieldKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(fieldService.createField(categoryId, field));
    }

    @PutMapping("/{fieldId}")
    public ResponseEntity<CategoryField> updateField(@PathVariable Long fieldId, @RequestBody CategoryField fieldDetails) {
        log.info("PUT /tutors/category-fields/{}", fieldId);
        return ResponseEntity.ok(fieldService.updateField(fieldId, fieldDetails));
    }

    @DeleteMapping("/{fieldId}")
    public ResponseEntity<Void> deleteField(@PathVariable Long fieldId) {
        log.info("DELETE /tutors/category-fields/{}", fieldId);
        fieldService.deleteField(fieldId);
        return ResponseEntity.noContent().build();
    }
}
