package com.superme.admin.controller;

import com.superme.dto.SchoolFormRequest;
import com.superme.dto.SchoolListItem;
import com.superme.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/schools")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SchoolController {

    private final SchoolService schoolService;

    // LIST – used by Schools table
    @GetMapping
    public ResponseEntity<List<SchoolListItem>> listSchools() {
        return ResponseEntity.ok(schoolService.list());
    }

    // GET BY ID – to load data into Edit School form
    @GetMapping("/{id}")
    public ResponseEntity<SchoolFormRequest> getSchool(@PathVariable Long id) {
        return ResponseEntity.ok(schoolService.getById(id));
    }

    // ADD – Add School
    @PostMapping
    public ResponseEntity<Long> createSchool(@RequestBody SchoolFormRequest request) {
        Long id = schoolService.create(request);
        return ResponseEntity.ok(id);
    }

    // EDIT – Edit School
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSchool(@PathVariable Long id,
                                             @RequestBody SchoolFormRequest request) {
        schoolService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    // DELETE – trash icon in ACTIONS column
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchool(@PathVariable Long id) {
        schoolService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
