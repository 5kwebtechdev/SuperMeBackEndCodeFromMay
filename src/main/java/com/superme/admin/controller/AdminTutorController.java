package com.superme.admin.controller;

import com.superme.dto.AdminTutorDTO;
import com.superme.dto.AdminTutorDTO.TutorStatistics;
import com.superme.dto.AdminTutorOverviewResponseDTO;
import com.superme.dto.TutorDto;
import com.superme.model.Tutor;
import com.superme.service.AdminTutorService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/tutors")
@CrossOrigin(origins = "*")
public class AdminTutorController {

    private final AdminTutorService adminTutorService;

    public AdminTutorController(AdminTutorService adminTutorService) {
        this.adminTutorService = adminTutorService;
    }

    // ================= OVERVIEW =================

    @GetMapping("/overview")
    public ResponseEntity<?> getAdminTutorOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchName,
            @RequestParam(required = false) String searchHeadline,
            @RequestParam(required = false) Integer searchAge,
            @RequestParam(required = false) String searchPhone,
            @RequestParam(required = false) List<Tutor.Subject> searchSubjects,
            @RequestParam(required = false) Tutor.Experience searchExperience,
            @RequestParam(required = false) String searchQualification,
            @RequestParam(required = false) Tutor.Gender searchGender,
            @RequestParam(required = false) String searchLocation,
            @RequestParam(required = false) List<Tutor.Experience> filterExperience,
            @RequestParam(required = false) List<String> filterQualification,
            @RequestParam(required = false) List<Tutor.Subject> filterSubjects,
            @RequestParam(required = false) List<String> filterLocation) {

        AdminTutorOverviewResponseDTO overview =
                adminTutorService.getAdminTutorOverview(
                        page, size, sortBy, sortDir,
                        searchName, searchHeadline, searchAge, searchPhone,
                        searchSubjects, searchExperience, searchQualification,
                        searchGender, searchLocation,
                        filterExperience, filterQualification, filterSubjects, filterLocation
                );

        return ResponseEntity.ok(Map.of("success", true, "data", overview));
    }

    // ================= DATA TABLE =================

    @GetMapping("/datatable")
    public ResponseEntity<?> getTutorDataTable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchName,
            @RequestParam(required = false) String searchHeadline,
            @RequestParam(required = false) Integer searchAge,
            @RequestParam(required = false) String searchPhone,
            @RequestParam(required = false) List<Tutor.Subject> searchSubjects,
            @RequestParam(required = false) Tutor.Experience searchExperience,
            @RequestParam(required = false) String searchQualification,
            @RequestParam(required = false) Tutor.Gender searchGender,
            @RequestParam(required = false) String searchLocation,
            @RequestParam(required = false) List<Tutor.Experience> filterExperience,
            @RequestParam(required = false) List<String> filterQualification,
            @RequestParam(required = false) List<Tutor.Subject> filterSubjects,
            @RequestParam(required = false) List<String> filterLocation) {

        Page<AdminTutorDTO> pageData =
                adminTutorService.getTutorDataTable(
                        page, size, sortBy, sortDir,
                        searchName, searchHeadline, searchAge, searchPhone,
                        searchSubjects, searchExperience, searchQualification,
                        searchGender, searchLocation,
                        filterExperience, filterQualification, filterSubjects, filterLocation
                );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", pageData.getContent(),
                "pagination", Map.of(
                        "currentPage", pageData.getNumber(),
                        "totalPages", pageData.getTotalPages(),
                        "totalElements", pageData.getTotalElements(),
                        "size", pageData.getSize(),
                        "hasNext", pageData.hasNext(),
                        "hasPrevious", pageData.hasPrevious()
                )
        ));
    }

    // ================= STATS =================

    @GetMapping("/stats")
    public ResponseEntity<?> getTutorStats() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.getTutorStatsForCards()
        ));
    }

    // ================= SEARCH =================

    @GetMapping("/search-suggestions")
    public ResponseEntity<?> getSearchSuggestions(@RequestParam String query,
                                                  @RequestParam String type) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.getSearchSuggestions(query, type)
        ));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<?> getFilterOptions() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.getFilterOptions(List.of())
        ));
    }

    // ================= CRUD =================

    @PostMapping("/add")
    public ResponseEntity<?> createTutor(@RequestBody TutorDto tutorDto) {
        Tutor tutor = adminTutorService.createTutor(tutorDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "data", tutor
        ));
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<?> createTutors(@RequestBody List<TutorDto> tutors) {
        List<Tutor> created = adminTutorService.createTutors(tutors);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "count", created.size()
        ));
    }

    @GetMapping
    public ResponseEntity<?> getAllTutors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<Tutor> pageData = adminTutorService.getAllTutors(page, size, sortBy, sortDir);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", pageData.getContent(),
                "pagination", Map.of(
                        "currentPage", pageData.getNumber(),
                        "totalPages", pageData.getTotalPages()
                )
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllTutorsNoPagination() {
        List<Tutor> tutors = adminTutorService.getAllTutors();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tutors
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTutorById(@PathVariable Long id) {
        Optional<AdminTutorDTO> tutor = adminTutorService.getTutorById(id);
        return tutor.map(value -> ResponseEntity.ok(Map.of("success", true, "data", value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Tutor not found")));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateTutor(@RequestBody TutorDto dto) {
        adminTutorService.updateTutor(dto);
        return ResponseEntity.ok(Map.of("success", true, "tutorId", dto.getId()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteTutor(@RequestParam Long id) {
        adminTutorService.deleteTutor(id);
        return ResponseEntity.ok(Map.of("success", true, "tutorId", id));
    }

    // ================= STATUS =================

    @PatchMapping("/activate")
    public ResponseEntity<?> activateTutor(@RequestParam Long id) {
        adminTutorService.activateTutor(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateTutor(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.deactivateTutor(id)
        ));
    }

    @PutMapping("/verify")
    public ResponseEntity<?> verifyTutor(@RequestParam Long id) {
        adminTutorService.verifyTutor(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/unverify")
    public ResponseEntity<?> unverifyTutor(@RequestParam Long id) {
        adminTutorService.unverifyTutor(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ================= FILE =================

    @PostMapping("/{id}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable Long id,
                                                  @RequestParam MultipartFile file) {

        String url = adminTutorService.uploadProfilePicture(id, file);
        return ResponseEntity.ok(Map.of("success", true, "fileUrl", url));
    }

    @PostMapping("/{id}/verification-documents")
    public ResponseEntity<?> uploadVerificationDocuments(@PathVariable Long id,
                                                         @RequestParam MultipartFile file) {

        String url = adminTutorService.uploadVerificationDocuments(id, file);
        return ResponseEntity.ok(Map.of("success", true, "fileUrl", url));
    }

    // ================= EXTRA =================

    @GetMapping("/by-status")
    public ResponseEntity<?> getTutorsByStatus(@RequestParam Boolean isActive) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.getTutorsByStatus(isActive)
        ));
    }

    @GetMapping("/by-subjects")
    public ResponseEntity<?> getTutorsBySubjects(@RequestParam List<Tutor.Subject> subjects) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.getTutorsBySubjects(subjects)
        ));
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getTutorStatistics() {
        TutorStatistics stats = adminTutorService.getTutorStatistics();
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    @GetMapping("/pending-verification")
    public ResponseEntity<?> getPendingVerificationTutors() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.getPendingVerificationTutors()
        ));
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentTutors(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.getRecentlyAddedTutors(limit)
        ));
    }

    @GetMapping("/enums")
    public ResponseEntity<?> getEnums() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminTutorService.getEnumValues()
        ));
    }

    // ================= BULK =================

    @PatchMapping("/bulk/activate")
    public ResponseEntity<?> bulkActivate(@RequestBody List<Long> ids) {
        ids.forEach(adminTutorService::activateTutor);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/bulk/deactivate")
    public ResponseEntity<?> bulkDeactivate(@RequestBody List<Long> ids) {
        ids.forEach(adminTutorService::deactivateTutor);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ================= EXPORT =================

    @GetMapping("/export")
    public ResponseEntity<?> exportTutorData(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String searchName,
            @RequestParam(required = false) String searchHeadline,
            @RequestParam(required = false) Integer searchAge,
            @RequestParam(required = false) String searchPhone,
            @RequestParam(required = false) List<Tutor.Subject> searchSubjects,
            @RequestParam(required = false) Tutor.Experience searchExperience,
            @RequestParam(required = false) String searchQualification,
            @RequestParam(required = false) Tutor.Gender searchGender,
            @RequestParam(required = false) String searchLocation,
            @RequestParam(required = false) List<Tutor.Experience> filterExperience,
            @RequestParam(required = false) List<String> filterQualification,
            @RequestParam(required = false) List<Tutor.Subject> filterSubjects,
            @RequestParam(required = false) List<String> filterLocation) {

        String data = adminTutorService.exportTutorData(
                format, searchName, searchHeadline, searchAge, searchPhone,
                searchSubjects, searchExperience, searchQualification,
                searchGender, searchLocation,
                filterExperience, filterQualification, filterSubjects, filterLocation
        );

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }
}