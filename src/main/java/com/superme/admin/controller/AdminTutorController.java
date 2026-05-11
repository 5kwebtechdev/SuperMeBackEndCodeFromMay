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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

//    @PostMapping("/add")
//    public ResponseEntity<?> createTutor(@RequestBody TutorDto tutorDto) {
//        Tutor tutor = adminTutorService.createTutor(tutorDto);
//        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
//                "success", true,
//                "data", tutor
//        ));
//    }


    @PostMapping(value = "/add", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createTutorWithFiles(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("gender") String gender,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityName") String entityName,
            @RequestParam("experience") String experience,
            @RequestParam("qualification") String qualification,
            @RequestParam("address") String address,
            @RequestParam("state") String state,
            @RequestParam("city") String city,
            @RequestParam("pincode") String pincode,
            @RequestParam("fees") BigDecimal fees,
            @RequestParam("age") Integer age,
            @RequestParam("priceType") String priceType,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam("level") String level,
            @RequestParam("time") String time,
            @RequestParam(value = "mode", required = false) List<String> modes,
            @RequestParam(value = "languages", required = false) List<String> languages,
            @RequestParam(value = "availableDays", required = false) List<String> availableDays,
            @RequestParam(value = "subjects", required = false) List<String> subjects,
            @RequestParam("category") String category,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestParam(value = "documents[]", required = false) List<MultipartFile> documents) {

        try {
            // LOG ALL INCOMING PARAMETERS
            System.out.println("========== RECEIVED FORM DATA ==========");
            System.out.println("name: " + name);
            System.out.println("email: " + email);
            System.out.println("phone: " + phone);
            System.out.println("gender: " + gender);
            System.out.println("entityType: " + entityType);
            System.out.println("entityName: " + entityName);
            System.out.println("experience: " + experience);
            System.out.println("qualification: " + qualification);
            System.out.println("address: " + address);
            System.out.println("state: " + state);
            System.out.println("city: " + city);
            System.out.println("pincode: " + pincode);
            System.out.println("fees: " + fees);
            System.out.println("age: " + age);
            System.out.println("priceType: " + priceType);
            System.out.println("startTime: " + startTime);
            System.out.println("endTime: " + endTime);
            System.out.println("level: " + level);
            System.out.println("time: " + time);
            System.out.println("modes: " + modes);
            System.out.println("languages: " + languages);
            System.out.println("availableDays: " + availableDays);
            System.out.println("subjects: " + subjects);
            System.out.println("category: " + category);
            System.out.println("profilePicture present: " + (profilePicture != null && !profilePicture.isEmpty()));
            System.out.println("documents count: " + (documents != null ? documents.size() : 0));
            System.out.println("========================================");

            // Create DTO from form parameters
            TutorDto tutorDto = new TutorDto();
            tutorDto.setName(name);
            tutorDto.setEmail(email);
            tutorDto.setPhone(phone);
            tutorDto.setAge(age);
            tutorDto.setQualification(qualification);
            tutorDto.setAddressLine(address);
            tutorDto.setState(state);
            tutorDto.setCity(city);
            tutorDto.setPincode(pincode);
            tutorDto.setFees(fees);

            // LOG DTO BEFORE SAVE
            System.out.println("========== DTO BEFORE VALIDATION ==========");
            System.out.println("DTO Name: " + tutorDto.getName());
            System.out.println("DTO Email: " + tutorDto.getEmail());
            System.out.println("DTO Phone: " + tutorDto.getPhone());
            System.out.println("DTO Age: " + tutorDto.getAge());
            System.out.println("DTO Qualification: " + tutorDto.getQualification());
            System.out.println("============================================");

            // Set gender
            try {
                tutorDto.setGender(Tutor.Gender.valueOf(gender.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.out.println("Gender mapping failed: " + gender);
                tutorDto.setGender(Tutor.Gender.MALE);
            }

            // Set entity type
            try {
                String entityTypeUpper = entityType.toUpperCase().replace(" ", "_");
                System.out.println("EntityType mapping: " + entityTypeUpper);
                tutorDto.setEntityType(Tutor.EntityType.valueOf(entityTypeUpper));
            } catch (IllegalArgumentException e) {
                System.out.println("EntityType mapping failed: " + entityType);
                tutorDto.setEntityType(Tutor.EntityType.INDIVIDUAL);
            }

            tutorDto.setEntityName(entityName);

            // Set experience
            try {
                String expUpper = experience.toUpperCase().replace("+", "PLUS").replace("-", "_").replace(" ", "_");
                System.out.println("Experience mapping: " + expUpper);
                tutorDto.setExperience(Tutor.Experience.valueOf(expUpper));
            } catch (IllegalArgumentException e) {
                System.out.println("Experience mapping failed: " + experience);
                tutorDto.setExperience(Tutor.Experience.PLUS_1);
            }

            // Set fee type
            try {
                String priceTypeUpper = priceType.toUpperCase().replace(" ", "_");
                System.out.println("PriceType mapping: " + priceTypeUpper);
                tutorDto.setFeeType(com.superme.enums.FeeType.valueOf(priceTypeUpper));
            } catch (IllegalArgumentException e) {
                System.out.println("PriceType mapping failed: " + priceType);
                tutorDto.setFeeType(com.superme.enums.FeeType.PER_HOUR);
            }

            // Parse times
            if (startTime != null && !startTime.isEmpty()) {
                java.time.LocalTime localTime = java.time.LocalTime.parse(startTime);
                tutorDto.setStartTime(java.time.LocalDateTime.of(java.time.LocalDate.now(), localTime));
            }
            if (endTime != null && !endTime.isEmpty()) {
                java.time.LocalTime localTime = java.time.LocalTime.parse(endTime);
                tutorDto.setEndTime(java.time.LocalDateTime.of(java.time.LocalDate.now(), localTime));
            }

            // Set levels
            if (level != null && !level.isEmpty()) {
//                tutorDto.setLevels(List.of(level));

                // TO THIS (mutable):
                List<String> levels = new ArrayList<>();
                levels.add(level);
                tutorDto.setLevels(levels);
            }

            // Set contact modes
            if (modes != null && !modes.isEmpty()) {
                List<Tutor.ContactMode> contactModes = modes.stream()
                        .map(m -> {
                            switch (m.toLowerCase()) {
                                case "online": return Tutor.ContactMode.VIDEO_CALL;
                                case "offline": return Tutor.ContactMode.IN_PERSON;
                                default: return Tutor.ContactMode.HYBRID;
                            }
                        })
                        .collect(Collectors.toList());
                tutorDto.setContactModes(contactModes);
            }

            // Set availability (days)
            if (availableDays != null && !availableDays.isEmpty()) {
                List<Tutor.Availability> availabilities = availableDays.stream()
                        .map(d -> {
                            switch (d.toLowerCase()) {
                                case "monday": return Tutor.Availability.MONDAY;
                                case "tuesday": return Tutor.Availability.TUESDAY;
                                case "wednesday": return Tutor.Availability.WEDNESDAY;
                                case "thursday": return Tutor.Availability.THURSDAY;
                                case "friday": return Tutor.Availability.FRIDAY;
                                case "saturday": return Tutor.Availability.SATURDAY;
                                case "sunday": return Tutor.Availability.SUNDAY;
                                default: return Tutor.Availability.MONDAY;
                            }
                        })
                        .collect(Collectors.toList());
                tutorDto.setAvailability(availabilities);
            }

            // Set subjects
            if (subjects != null && !subjects.isEmpty()) {
                List<Tutor.Subject> subjectList = subjects.stream()
                        .map(s -> {
                            switch (s.toLowerCase()) {
                                case "mathematics": return Tutor.Subject.MATHEMATICS;
                                case "science": return Tutor.Subject.SCIENCE;
                                case "english": return Tutor.Subject.ENGLISH;
                                case "physics": return Tutor.Subject.PHYSICS;
                                case "chemistry": return Tutor.Subject.CHEMISTRY;
                                case "biology": return Tutor.Subject.BIOLOGY;
                                default: return Tutor.Subject.MATHEMATICS;
                            }
                        })
                        .collect(Collectors.toList());
                tutorDto.setSubjects(subjectList);
            }

            tutorDto.setLocation(city + ", " + state);

            // LOG FINAL DTO
            System.out.println("========== FINAL DTO BEFORE CREATE ==========");
            System.out.println("DTO: " + tutorDto);
            System.out.println("==============================================");

            // Create tutor
            Tutor createdTutor = adminTutorService.createTutor(tutorDto);

            System.out.println("========== TUTOR CREATED SUCCESSFULLY ==========");
            System.out.println("Tutor ID: " + createdTutor.getId());
            System.out.println("=================================================");

            // Upload profile picture if provided
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String profileUrl = adminTutorService.uploadProfilePicture(createdTutor.getId(), profilePicture);
                createdTutor.setProfilePicUrl(profileUrl);
            }

            // Upload documents if provided
            if (documents != null && !documents.isEmpty()) {
                List<String> documentUrls = new ArrayList<>();
                for (MultipartFile doc : documents) {
                    String docUrl = adminTutorService.uploadVerificationDocuments(createdTutor.getId(), doc);
                    documentUrls.add(docUrl);
                }
                if (!documentUrls.isEmpty()) {
                    createdTutor.setDocumentsVerificationUrl(String.join(",", documentUrls));
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "data", createdTutor,
                    "message", "Tutor added successfully"
            ));

        } catch (Exception e) {
            System.out.println("========== ERROR CREATING TUTOR ==========");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("Error cause: " + e.getCause());
            e.printStackTrace();  // This will print the full stack trace
            System.out.println("===========================================");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Failed to create tutor: " + e.getMessage()
            ));
        }
    }

    // Helper method to convert Tutor to TutorDto
    private TutorDto convertToDto(Tutor tutor) {
        TutorDto dto = new TutorDto();
        dto.setId(tutor.getId());
        dto.setName(tutor.getName());
        dto.setEmail(tutor.getEmail());
        dto.setPhone(tutor.getPhone());
        dto.setGender(tutor.getGender());
        dto.setEntityType(tutor.getEntityType());
        dto.setEntityName(tutor.getEntityName());
        dto.setExperience(tutor.getExperience());
        dto.setQualification(tutor.getQualification());
        dto.setAddressLine(tutor.getAddressLine());
        dto.setState(tutor.getState());
        dto.setCity(tutor.getCity());
        dto.setPincode(tutor.getPincode());
        dto.setFees(tutor.getFees());
        dto.setFeeType(tutor.getFeeType());
        dto.setStartTime(tutor.getStartTime());
        dto.setEndTime(tutor.getEndTime());
        dto.setLevels(tutor.getLevels());
        dto.setContactModes(tutor.getContactModes());
        dto.setAvailability(tutor.getAvailability());
        dto.setSubjects(tutor.getSubjects());
        dto.setLocation(tutor.getLocation());
        dto.setProfilePicUrl(tutor.getProfilePicUrl());
        dto.setDocumentsVerificationUrl(tutor.getDocumentsVerificationUrl());
        return dto;
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