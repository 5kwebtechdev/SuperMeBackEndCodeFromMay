package com.superme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superme.admin.dto.TutorCategoryMappingDto;
import com.superme.dto.AdminTutorDTO;
import com.superme.dto.AdminTutorOverviewResponseDTO;
import com.superme.dto.TutorDto;
import com.superme.model.Tutor;
import com.superme.model.TutorCategoryMapping;
import com.superme.repository.TutorCategoryMappingRepository;
import com.superme.repository.TutorRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for Admin Tutor operations with comprehensive screen
 * functionality.
 * Handles data table, stat cards, search, filters, and CRUD operations.
 */
@Service
@Slf4j
public class AdminTutorService {

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private TutorCategoryMappingRepository tutorCategoryMappingRepository;

    // ============================================================================
    // MAIN ADMIN SCREEN METHODS
    // ============================================================================

    /**
     * Get complete admin tutor overview with data table, stats, and filters
     */
    @Transactional
    public AdminTutorOverviewResponseDTO getAdminTutorOverview(
            int page, int size, String sortBy, String sortDir,
            String searchName, String searchHeadline, Integer searchAge, String searchPhone,
            List<Tutor.Subject> searchSubjects, Tutor.Experience searchExperience,
            String searchQualification, Tutor.Gender searchGender, String searchLocation,
            List<Tutor.Experience> filterExperience, List<String> filterQualification,
            List<Tutor.Subject> filterSubjects, List<String> filterLocation) {

        Page<AdminTutorDTO> dataTable = getTutorDataTable(
                page, size, sortBy, sortDir, searchName, searchHeadline, searchAge, searchPhone,
                searchSubjects, searchExperience, searchQualification, searchGender,
                searchLocation, filterExperience, filterQualification, filterSubjects, filterLocation);

        Map<String, Object> stats = getTutorStatsForCards();
        Map<String, Object> filterOptions = getFilterOptions(dataTable.getContent());

        AdminTutorOverviewResponseDTO response = new AdminTutorOverviewResponseDTO();
        response.setTutors(dataTable.getContent());
        response.setStats(stats);
        response.setFilterOptions(filterOptions);
        response.setTotalElements(dataTable.getTotalElements());
        response.setTotalPages(dataTable.getTotalPages());
        response.setCurrentPage(dataTable.getNumber());
        response.setPageSize(dataTable.getSize());
        response.setHasNext(dataTable.hasNext());
        response.setHasPrevious(dataTable.hasPrevious());

        return response;
    }

    /**
     * Get data table with search and filters (in‑memory filtering)
     */
    @Transactional
    public Page<AdminTutorDTO> getTutorDataTable(
            int page, int size, String sortBy, String sortDir,
            String searchName, String searchHeadline, Integer searchAge, String searchPhone,
            List<Tutor.Subject> searchSubjects, Tutor.Experience searchExperience,
            String searchQualification, Tutor.Gender searchGender, String searchLocation,
            List<Tutor.Experience> filterExperience, List<String> filterQualification,
            List<Tutor.Subject> filterSubjects, List<String> filterLocation) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        List<Tutor> allTutors = tutorRepository.findAll();
        List<Tutor> filtered = allTutors.stream()
                .filter(tutor -> applySearchCriteria(tutor, searchName, searchHeadline, searchAge, searchPhone,
                        searchSubjects, searchExperience, searchQualification, searchGender, searchLocation))
                .filter(tutor -> applyFilterCriteria(tutor, filterExperience, filterQualification, filterSubjects,
                        filterLocation))
                .sorted((t1, t2) -> {
                    Comparable v1 = getComparableField(t1, sortBy);
                    Comparable v2 = getComparableField(t2, sortBy);
                    int cmp = v1 == null ? (v2 == null ? 0 : -1) : (v2 == null ? 1 : v1.compareTo(v2));
                    return sortDir.equalsIgnoreCase("desc") ? -cmp : cmp;
                })
                .collect(Collectors.toList());

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());

        List<AdminTutorDTO> pageContent = filtered.subList(start, end).stream()
                .map(this::convertToAdminTutorDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    /**
     * Get stats for stat cards
     */
    public Map<String, Object> getTutorStatsForCards() {
        Map<String, Object> stats = new HashMap<>();

        long totalTutors = tutorRepository.count();
        long activeTutors = tutorRepository.countByIsActiveTrue();
        long verifiedTutors = tutorRepository.countByIsVerifiedTrue();
        long unverifiedTutors = totalTutors - verifiedTutors;

        stats.put("totalTutors", totalTutors);
        stats.put("activeTutors", activeTutors);
        stats.put("verifiedTutors", verifiedTutors);
        stats.put("unverifiedTutors", unverifiedTutors);

        return stats;
    }

    /**
     * Get search suggestions for search bar
     */
    public List<String> getSearchSuggestions(String query, String type) {
        List<String> suggestions = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase();

        switch (type.toLowerCase()) {
            case "name" -> suggestions = tutorRepository.findAll().stream()
                    .map(Tutor::getName)
                    .filter(name -> name != null && name.toLowerCase().contains(q))
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
            case "headline" -> suggestions = tutorRepository.findAll().stream()
                    .map(Tutor::getHeadline)
                    .filter(headline -> headline != null && headline.toLowerCase().contains(q))
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
            case "location" -> suggestions = tutorRepository.findAll().stream()
                    .map(Tutor::getLocation)
                    .filter(location -> location != null && location.toLowerCase().contains(q))
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
            case "qualification" -> suggestions = tutorRepository.findAll().stream()
                    .map(Tutor::getQualification)
                    .filter(qual -> qual != null && qual.toLowerCase().contains(q))
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
        }

        return suggestions;
    }

    /**
     * Get filter options for filter dropdowns
     */
    public Map<String, Object> getFilterOptions(List<AdminTutorDTO> tutors) {
        Map<String, Object> options = new HashMap<>();

        options.put("experiences", Arrays.asList(Tutor.Experience.values()));

        Set<String> qualifications = tutors.stream()
                .map(AdminTutorDTO::getQualification)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        options.put("qualifications", qualifications);

        options.put("subjects", Arrays.asList(Tutor.Subject.values()));

        Set<String> locations = tutors.stream()
                .map(AdminTutorDTO::getLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        options.put("locations", locations);

        return options;
    }

    // ============================================================================
    // CRUD OPERATIONS
    // ============================================================================

    /**
     * Create a new tutor
     */
//    @Transactional
//    public Tutor createTutor(TutorDto request) {
//        Tutor tutor = Tutor.builder()
//                .name(request.getName())
//                .headline(request.getHeadline())
//                .age(request.getAge())
//                .phone(request.getPhone())
//                .email(request.getEmail())
//                .gender(request.getGender())
//                .qualification(request.getQualification())
//                .experience(request.getExperience())
//                .entityType(request.getEntityType())
//                .entityName(request.getEntityName())
//                .subjects(request.getSubjects())
//                .location(request.getLocation())
//                // NEW address fields
//                .addressLine(request.getAddressLine())
//                .state(request.getState())
//                .city(request.getCity())
//                .pincode(request.getPincode())
//                .profilePicUrl(request.getProfilePicUrl())
//                .startTime(request.getStartTime())
//                .endTime(request.getEndTime())
//                .levels(request.getLevels())
//                .documentsVerificationUrl(request.getDocumentsVerificationUrl())
//                .hourlyRate(request.getHourlyRate())
//                .availability(request.getAvailability())
//                .contactModes(request.getContactModes())
//                .feeType(request.getFeeType())
//                .fees(request.getFees())
//                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
//                .isVerified(false)
//                .build();
//
//        Tutor savedTutor = tutorRepository.saveAndFlush(tutor);
//
//        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
//            List<TutorCategoryMapping> categoryMappings = new ArrayList<>();
//            ObjectMapper mapper = new ObjectMapper();
//
//            for (TutorCategoryMappingDto categoryDto : request.getCategories()) {
//                try {
//                    String fieldOptionsJson = mapper.writeValueAsString(categoryDto.getFieldOptions());
//
//                    TutorCategoryMapping mapping = TutorCategoryMapping.builder()
//                            .tutorId(savedTutor.getId())
//                            .categoryId(categoryDto.getCategoryId())
//                            .selectedFieldOptions(fieldOptionsJson)
//                            .expertiseLevel(categoryDto.getExpertiseLevel())
//                            .yearsOfExperience(categoryDto.getYearsOfExperience())
//                            .isAcceptingStudents(categoryDto.getIsAcceptingStudents() != null
//                                    ? categoryDto.getIsAcceptingStudents()
//                                    : true)
//                            .maxStudentsPerBatch(categoryDto.getMaxStudentsPerBatch())
//                            .categoryHourlyRate(categoryDto.getCategoryHourlyRate())
//                            .categoryBatchRate(categoryDto.getCategoryBatchRate())
//                            .description(categoryDto.getDescription())
//                            .build();
//
//                    categoryMappings.add(mapping);
//
//                } catch (Exception ex) {
//                    log.warn("Error processing category {} for tutor {}: {}",
//                            categoryDto.getCategoryId(), savedTutor.getId(), ex.getMessage());
//                }
//            }
//
//            if (!categoryMappings.isEmpty()) {
//                tutorCategoryMappingRepository.saveAll(categoryMappings);
//                savedTutor.setCategoryMappings(categoryMappings);
//            }
//        }
//
//        return savedTutor;
//    }


    @Autowired
    private FileStorageService2 fileStorageService2;

    @Transactional
    public Tutor createTutor(TutorDto request) {


        System.out.println("========== CREATE TUTOR SERVICE ==========");
        System.out.println("Request Name: " + request.getName());
        System.out.println("Request Email: " + request.getEmail());
        System.out.println("Request Phone: " + request.getPhone());
        System.out.println("Request Age: " + request.getAge());
        System.out.println("Request Gender: " + request.getGender());
        System.out.println("Request Qualification: " + request.getQualification());
        System.out.println("Request Experience: " + request.getExperience());
        System.out.println("Request HourlyRate: " + request.getHourlyRate());
        System.out.println("Request Headline: " + request.getHeadline());
        System.out.println("===========================================");


        // STEP 1: Create tutor WITHOUT element collections
        Tutor tutor = Tutor.builder()
                .name(request.getName())
                .headline(request.getHeadline())
                .age(request.getAge())
                .phone(request.getPhone())
                .email(request.getEmail())
                .gender(request.getGender())
                .qualification(request.getQualification())
                .experience(request.getExperience())
                .entityType(request.getEntityType())
                .entityName(request.getEntityName())
                .location(request.getLocation())

                // Address
                .addressLine(request.getAddressLine())
                .state(request.getState())
                .city(request.getCity())
                .pincode(request.getPincode())

                .profilePicUrl(request.getProfilePicUrl())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timePreference(request.getTimePreference())

                .documentsVerificationUrl(request.getDocumentsVerificationUrl())
                .hourlyRate(request.getHourlyRate())

                .feeType(request.getFeeType())
                .fees(request.getFees())

                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isVerified(false)
                .build();

        // STEP 2: Save tutor FIRST (generate ID)
        Tutor savedTutor = tutorRepository.saveAndFlush(tutor);

        // STEP 3: NOW safely set ElementCollections
        if (request.getLevels() != null && !request.getLevels().isEmpty()) {
            savedTutor.setLevels(request.getLevels());
        }

        if (request.getAvailability() != null && !request.getAvailability().isEmpty()) {
            savedTutor.setAvailability(request.getAvailability());
        }
        if (request.getSubjects() != null && !request.getSubjects().isEmpty()) {
            savedTutor.setSubjects(request.getSubjects());
        }

        if (request.getContactModes() != null && !request.getContactModes().isEmpty()) {
            savedTutor.setContactModes(request.getContactModes());
        }
        if (request.getLanguages() != null && !request.getLanguages().isEmpty()) {
            savedTutor.setLanguages(request.getLanguages());
        }
        if (request.getBoards() != null && !request.getBoards().isEmpty()) {
            savedTutor.setBoards(request.getBoards());
        }
        if (request.getClasses() != null && !request.getClasses().isEmpty()) {
            savedTutor.setClasses(request.getClasses());
        }
        if (request.getDegrees() != null && !request.getDegrees().isEmpty()) {
            savedTutor.setDegrees(request.getDegrees());
        }
        if (request.getYears() != null && !request.getYears().isEmpty()) {
            savedTutor.setYears(request.getYears());
        }
        if (request.getLanguagesOffered() != null && !request.getLanguagesOffered().isEmpty()) {
            savedTutor.setLanguagesOffered(request.getLanguagesOffered());
        }
        if (request.getProficiencyLevels() != null && !request.getProficiencyLevels().isEmpty()) {
            savedTutor.setProficiencyLevels(request.getProficiencyLevels());
        }
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            savedTutor.setSkills(request.getSkills());
        }
        if (request.getHobbyProficiency() != null && !request.getHobbyProficiency().isEmpty()) {
            savedTutor.setHobbyProficiency(request.getHobbyProficiency());
        }
        if (request.getAgeGroups() != null && !request.getAgeGroups().isEmpty()) {
            savedTutor.setAgeGroups(request.getAgeGroups());
        }
        if (request.getTargetExams() != null && !request.getTargetExams().isEmpty()) {
            savedTutor.setTargetExams(request.getTargetExams());
        }
        if (request.getActivities() != null && !request.getActivities().isEmpty()) {
            savedTutor.setActivities(request.getActivities());
        }
        if (request.getOtherSkills() != null && !request.getOtherSkills().isEmpty()) {
            savedTutor.setOtherSkills(request.getOtherSkills());
        }
        if (request.getOtherLevels() != null && !request.getOtherLevels().isEmpty()) {
            savedTutor.setOtherLevels(request.getOtherLevels());
        }
        // STEP 4: Save again to persist collections
        savedTutor = tutorRepository.save(savedTutor);

        // ================= CATEGORY MAPPINGS =================

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {

            List<TutorCategoryMapping> categoryMappings = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();

            for (TutorCategoryMappingDto categoryDto : request.getCategories()) {
                try {
                    String fieldOptionsJson =
                            mapper.writeValueAsString(categoryDto.getFieldOptions());

                    TutorCategoryMapping mapping = TutorCategoryMapping.builder()
                            .tutorId(savedTutor.getId()) // FK safe now
                            .categoryId(categoryDto.getCategoryId())
                            .selectedFieldOptions(fieldOptionsJson)
                            .expertiseLevel(categoryDto.getExpertiseLevel())
                            .yearsOfExperience(categoryDto.getYearsOfExperience())
                            .isAcceptingStudents(
                                    categoryDto.getIsAcceptingStudents() != null
                                            ? categoryDto.getIsAcceptingStudents()
                                            : true
                            )
                            .maxStudentsPerBatch(categoryDto.getMaxStudentsPerBatch())
                            .categoryHourlyRate(categoryDto.getCategoryHourlyRate())
                            .categoryBatchRate(categoryDto.getCategoryBatchRate())
                            .description(categoryDto.getDescription())
                            .build();

                    categoryMappings.add(mapping);

                } catch (Exception ex) {
                    log.warn("Error processing category {} for tutor {}: {}",
                            categoryDto.getCategoryId(),
                            savedTutor.getId(),
                            ex.getMessage());
                }
            }

//            if (!categoryMappings.isEmpty()) {
//                tutorCategoryMappingRepository.saveAll(categoryMappings);
//                savedTutor.setCategoryMappings(categoryMappings);
//            }
            if (!categoryMappings.isEmpty()) {

                // IMPORTANT: use existing list, don't replace
                savedTutor.getCategoryMappings().clear();

                for (TutorCategoryMapping mapping : categoryMappings) {

                    // VERY IMPORTANT (fix relation)
                    mapping.setTutorId(savedTutor.getId());

                    savedTutor.getCategoryMappings().add(mapping);
                }
            }
        }

        return tutorRepository.save(savedTutor);
    }

    /**
     * Update tutor
     */
    @Transactional
    public Tutor updateTutor(TutorDto tutorDto) {
        Tutor existingTutor = tutorRepository.findById(tutorDto.getId())
                .orElseThrow(() -> new RuntimeException("Tutor not found with id: " + tutorDto.getId()));

        if (tutorDto.getName() != null) existingTutor.setName(tutorDto.getName());
        if (tutorDto.getHeadline() != null) existingTutor.setHeadline(tutorDto.getHeadline());
        if (tutorDto.getAge() != null) existingTutor.setAge(tutorDto.getAge());
        if (tutorDto.getPhone() != null) existingTutor.setPhone(tutorDto.getPhone());
        if (tutorDto.getEmail() != null) existingTutor.setEmail(tutorDto.getEmail());
        if (tutorDto.getGender() != null) existingTutor.setGender(tutorDto.getGender());
        if (tutorDto.getQualification() != null) existingTutor.setQualification(tutorDto.getQualification());
        if (tutorDto.getExperience() != null) existingTutor.setExperience(tutorDto.getExperience());
        if (tutorDto.getEntityType() != null) existingTutor.setEntityType(tutorDto.getEntityType());
        if (tutorDto.getEntityName() != null) existingTutor.setEntityName(tutorDto.getEntityName());
        if (tutorDto.getSubjects() != null) existingTutor.setSubjects(tutorDto.getSubjects());
        if (tutorDto.getLocation() != null) existingTutor.setLocation(tutorDto.getLocation());
        // NEW address fields
        if (tutorDto.getAddressLine() != null) existingTutor.setAddressLine(tutorDto.getAddressLine());
        if (tutorDto.getState() != null) existingTutor.setState(tutorDto.getState());
        if (tutorDto.getCity() != null) existingTutor.setCity(tutorDto.getCity());
        if (tutorDto.getPincode() != null) existingTutor.setPincode(tutorDto.getPincode());
        if (tutorDto.getProfilePicUrl() != null) existingTutor.setProfilePicUrl(tutorDto.getProfilePicUrl());
        if (tutorDto.getStartTime() != null) existingTutor.setStartTime(tutorDto.getStartTime());
        if (tutorDto.getEndTime() != null) existingTutor.setEndTime(tutorDto.getEndTime());
        if (tutorDto.getLevels() != null) existingTutor.setLevels(tutorDto.getLevels());
        if (tutorDto.getDocumentsVerificationUrl() != null)
            existingTutor.setDocumentsVerificationUrl(tutorDto.getDocumentsVerificationUrl());
        if (tutorDto.getHourlyRate() != null) existingTutor.setHourlyRate(tutorDto.getHourlyRate());
        if (tutorDto.getAvailability() != null) existingTutor.setAvailability(tutorDto.getAvailability());
        if (tutorDto.getContactModes() != null) existingTutor.setContactModes(tutorDto.getContactModes());
        if (tutorDto.getFeeType() != null) existingTutor.setFeeType(tutorDto.getFeeType());
        if (tutorDto.getFees() != null) existingTutor.setFees(tutorDto.getFees());
        if (tutorDto.getIsActive() != null) existingTutor.setIsActive(tutorDto.getIsActive());
        if (tutorDto.getTimePreference() != null) existingTutor.setTimePreference(tutorDto.getTimePreference());
        if (tutorDto.getLanguages() != null) existingTutor.setLanguages(tutorDto.getLanguages());
        if (tutorDto.getBoards() != null) existingTutor.setBoards(tutorDto.getBoards());
        if (tutorDto.getClasses() != null) existingTutor.setClasses(tutorDto.getClasses());
        if (tutorDto.getDegrees() != null) existingTutor.setDegrees(tutorDto.getDegrees());
        if (tutorDto.getYears() != null) existingTutor.setYears(tutorDto.getYears());
        if (tutorDto.getLanguagesOffered() != null) existingTutor.setLanguagesOffered(tutorDto.getLanguagesOffered());
        if (tutorDto.getProficiencyLevels() != null) existingTutor.setProficiencyLevels(tutorDto.getProficiencyLevels());
        if (tutorDto.getSkills() != null) existingTutor.setSkills(tutorDto.getSkills());
        if (tutorDto.getHobbyProficiency() != null) existingTutor.setHobbyProficiency(tutorDto.getHobbyProficiency());
        if (tutorDto.getAgeGroups() != null) existingTutor.setAgeGroups(tutorDto.getAgeGroups());
        if (tutorDto.getTargetExams() != null) existingTutor.setTargetExams(tutorDto.getTargetExams());
        if (tutorDto.getActivities() != null) existingTutor.setActivities(tutorDto.getActivities());
        if (tutorDto.getOtherSkills() != null) existingTutor.setOtherSkills(tutorDto.getOtherSkills());
        if (tutorDto.getOtherLevels() != null) existingTutor.setOtherLevels(tutorDto.getOtherLevels());

        existingTutor.setUpdatedAt(LocalDateTime.now());
        return tutorRepository.save(existingTutor);
    }

    /**
     * Bulk create tutors
     */
    @Transactional
    public List<Tutor> createTutors(List<TutorDto> tutorDtos) {
        return tutorDtos.stream()
                .map(this::createTutor)
                .collect(Collectors.toList());
    }

    public Page<Tutor> getAllTutors(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return tutorRepository.findAll(pageable);
    }

    public List<Tutor> getAllTutors() {
        return tutorRepository.findAll();
    }

//    public Optional<AdminTutorDTO> getTutorById(Long id) {
//        return tutorRepository.findById(id).map(this::convertToAdminTutorDTO);
//    }

    @Transactional
    public void deleteTutor(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor not found with id: " + id));
        tutor.setIsActive(false);
        tutor.setUpdatedAt(LocalDateTime.now());
        tutorRepository.save(tutor);
    }

    // ============================================================================
    // STATUS MANAGEMENT
    // ============================================================================

    @Transactional
    public Tutor activateTutor(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor not found with id: " + id));
        tutor.setIsActive(true);
        tutor.setUpdatedAt(LocalDateTime.now());
        return tutorRepository.save(tutor);
    }

    @Transactional
    public Tutor deactivateTutor(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor not found with id: " + id));
        tutor.setIsActive(false);
        tutor.setUpdatedAt(LocalDateTime.now());
        return tutorRepository.save(tutor);
    }

    @Transactional
    public Tutor verifyTutor(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor not found with id: " + id));
        tutor.setIsVerified(true);
        tutor.setUpdatedAt(LocalDateTime.now());
        return tutorRepository.save(tutor);
    }

    @Transactional
    public Tutor unverifyTutor(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor not found with id: " + id));
        tutor.setIsVerified(false);
        tutor.setUpdatedAt(LocalDateTime.now());
        return tutorRepository.save(tutor);
    }

    // ============================================================================
    // FILE UPLOAD OPERATIONS
    // ============================================================================


    @Autowired
    private  FileStorageService fileStorageService;

//    public String uploadProfilePicture(Long id, MultipartFile file) {
//
//        String filePath = fileStorageService.saveFile(id, file, "profile");
//
//        tutorRepository.findById(id).ifPresent(tutor -> {
//            tutor.setProfilePicUrl(filePath);
//            tutor.setUpdatedAt(LocalDateTime.now());
//            tutorRepository.save(tutor);
//        });
//
//        return filePath;
//    }

//    public String uploadVerificationDocuments(Long id, MultipartFile file) {
//
//        String filePath = fileStorageService.saveFile(id, file, "documents");
//
//        tutorRepository.findById(id).ifPresent(tutor -> {
//            tutor.setDocumentsVerificationUrl(filePath);
//            tutor.setUpdatedAt(LocalDateTime.now());
//            tutorRepository.save(tutor);
//        });
//
//        return filePath;
//    }

    // ============================================================================
    // SEARCH AND FILTER OPERATIONS
    // ============================================================================

    /**
     * Search tutors by name or headline (in‑memory)
     */
    public List<Tutor> searchTutors(String query) {
        if (query == null || query.trim().isEmpty()) {
            return tutorRepository.findAll();
        }
        String q = query.toLowerCase();
        return tutorRepository.findAll().stream()
                .filter(tutor -> {
                    boolean nameMatch = tutor.getName() != null &&
                            tutor.getName().toLowerCase().contains(q);
                    boolean headlineMatch = tutor.getHeadline() != null &&
                            tutor.getHeadline().toLowerCase().contains(q);
                    return nameMatch || headlineMatch;
                })
                .collect(Collectors.toList());
    }

    public List<Tutor> getTutorsByStatus(Boolean isActive) {
        return (isActive == null) ? tutorRepository.findAll() : tutorRepository.findByIsActive(isActive);
    }

//    public List<Tutor> getTutorsBySubjects(List<Tutor.Subject> subjects) {
//        if (subjects == null || subjects.isEmpty()) {
//            return tutorRepository.findAll();
//        }
//        return tutorRepository.findBySubjectsIn(subjects);
//    }
public List<Tutor> getTutorsBySubjects(List<Tutor.Subject> subjects) {

    if (subjects == null || subjects.isEmpty()) {
        return tutorRepository.findAll();
    }

    return tutorRepository.findBySubjectsIn(subjects);
}
    public AdminTutorDTO.TutorStatistics getTutorStatistics() {
        long totalTutors = tutorRepository.count();
        long activeTutors = tutorRepository.countByIsActiveTrue();
        long verifiedTutors = tutorRepository.countByIsVerifiedTrue();

        AdminTutorDTO.TutorStatistics stats = new AdminTutorDTO.TutorStatistics();
        stats.setTotalTutors(totalTutors);
        stats.setActiveTutors(activeTutors);
        stats.setVerifiedTutors(verifiedTutors);
        return stats;
    }

    public List<Tutor> getPendingVerificationTutors() {
        return tutorRepository.findByIsVerifiedFalse();
    }

    public List<Tutor> getRecentlyAddedTutors(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return tutorRepository.findAll(pageable).getContent();
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    public Map<String, Object> getEnumValues() {
        Map<String, Object> enums = new HashMap<>();
        enums.put("genders", Arrays.asList(Tutor.Gender.values()));
        enums.put("experiences", Arrays.asList(Tutor.Experience.values()));
        enums.put("entityTypes", Arrays.asList(Tutor.EntityType.values()));
        enums.put("subjects", Arrays.asList(Tutor.Subject.values()));
        enums.put("availabilities", Arrays.asList(Tutor.Availability.values()));
        enums.put("contactModes", Arrays.asList(Tutor.ContactMode.values()));
        return enums;
    }

    @Transactional
    public String exportTutorData(String format, String searchName, String searchHeadline, Integer searchAge,
                                  String searchPhone, List<Tutor.Subject> searchSubjects, Tutor.Experience searchExperience,
                                  String searchQualification, Tutor.Gender searchGender, String searchLocation,
                                  List<Tutor.Experience> filterExperience, List<String> filterQualification,
                                  List<Tutor.Subject> filterSubjects, List<String> filterLocation) {

        List<Tutor> tutors = tutorRepository.findAll().stream()
                .filter(tutor -> applySearchCriteria(tutor, searchName, searchHeadline, searchAge, searchPhone,
                        searchSubjects, searchExperience, searchQualification, searchGender, searchLocation))
                .filter(tutor -> applyFilterCriteria(tutor, filterExperience, filterQualification,
                        filterSubjects, filterLocation))
                .collect(Collectors.toList());

        return "csv".equalsIgnoreCase(format) ? exportToCsv(tutors) : exportToJson(tutors);
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    private Comparable<?> getComparableField(Tutor tutor, String field) {
        return switch (field) {
            case "name" -> tutor.getName();
            case "headline" -> tutor.getHeadline();
            case "age" -> tutor.getAge();
            case "qualification" -> tutor.getQualification();
            case "location" -> tutor.getLocation();
            case "experience" -> tutor.getExperience() != null ? tutor.getExperience().toString() : null;
            case "entityName" -> tutor.getEntityName();
            case "hourlyRate" -> tutor.getHourlyRate();
            default -> null;
        };
    }

    private boolean applySearchCriteria(Tutor tutor, String searchName, String searchHeadline, Integer searchAge,
                                        String searchPhone, List<Tutor.Subject> searchSubjects, Tutor.Experience searchExperience,
                                        String searchQualification, Tutor.Gender searchGender, String searchLocation) {

        if (searchName != null && tutor.getName() != null &&
                !tutor.getName().toLowerCase().contains(searchName.toLowerCase())) {
            return false;
        }
        if (searchHeadline != null && tutor.getHeadline() != null &&
                !tutor.getHeadline().toLowerCase().contains(searchHeadline.toLowerCase())) {
            return false;
        }
        if (searchAge != null && !tutor.getAge().equals(searchAge)) {
            return false;
        }
        if (searchPhone != null && tutor.getPhone() != null && !tutor.getPhone().contains(searchPhone)) {
            return false;
        }
        if (searchSubjects != null && !searchSubjects.isEmpty() && tutor.getSubjects() != null) {
            boolean hasSubject = tutor.getSubjects().stream().anyMatch(searchSubjects::contains);
            if (!hasSubject) return false;
        }
        if (searchExperience != null && !tutor.getExperience().equals(searchExperience)) {
            return false;
        }
        if (searchQualification != null && tutor.getQualification() != null &&
                !tutor.getQualification().toLowerCase().contains(searchQualification.toLowerCase())) {
            return false;
        }
        if (searchGender != null && !tutor.getGender().equals(searchGender)) {
            return false;
        }
        return searchLocation == null || (tutor.getLocation() != null &&
                tutor.getLocation().toLowerCase().contains(searchLocation.toLowerCase()));
    }

    private boolean applyFilterCriteria(Tutor tutor, List<Tutor.Experience> filterExperience,
                                        List<String> filterQualification, List<Tutor.Subject> filterSubjects, List<String> filterLocation) {

        if (filterExperience != null && !filterExperience.isEmpty() &&
                tutor.getExperience() != null && !filterExperience.contains(tutor.getExperience())) {
            return false;
        }
        if (filterQualification != null && !filterQualification.isEmpty() &&
                tutor.getQualification() != null && !filterQualification.contains(tutor.getQualification())) {
            return false;
        }
        if (filterSubjects != null && !filterSubjects.isEmpty() && tutor.getSubjects() != null) {
            boolean hasSubject = tutor.getSubjects().stream().anyMatch(filterSubjects::contains);
            if (!hasSubject) return false;
        }
        return filterLocation == null || filterLocation.isEmpty() ||
                (tutor.getLocation() != null && filterLocation.contains(tutor.getLocation()));
    }

    private AdminTutorDTO convertToAdminTutorDTO(Tutor tutor) {
        AdminTutorDTO dto = new AdminTutorDTO();
        dto.setId(tutor.getId());
        dto.setName(tutor.getName());
        dto.setHeadline(tutor.getHeadline());
        dto.setAge(tutor.getAge());
        dto.setGender(tutor.getGender() != null ? tutor.getGender().getDisplayName() : null);
        dto.setExperience(tutor.getExperience() != null ? tutor.getExperience().getDisplayName() : null);
        dto.setQualification(tutor.getQualification());
        dto.setPhone(tutor.getPhone());
        dto.setEmail(tutor.getEmail());
        dto.setDocumentsVerified(tutor.getIsVerified());
        dto.setHourlyRate(tutor.getHourlyRate() != null ? tutor.getHourlyRate().doubleValue() : null);
        dto.setLocation(tutor.getLocation());
        dto.setAddressLine(tutor.getAddressLine());
        dto.setState(tutor.getState());
        dto.setCity(tutor.getCity());
        dto.setPincode(tutor.getPincode());
        dto.setSubjects(tutor.getSubjects() != null
                ? tutor.getSubjects().stream().map(Tutor.Subject::getDisplayName).collect(Collectors.toList())
                : new ArrayList<>());
        dto.setEntityType(tutor.getEntityType() != null ? tutor.getEntityType().getDisplayName() : null);
        dto.setEntityName(tutor.getEntityName());
        dto.setStatus(tutor.isActive() ? "Active" : "Inactive");
        dto.setVerificationStatus(tutor.isVerified() ? "Verified" : "Pending");
        dto.setCreatedAt(tutor.getCreatedAt());
        dto.setUpdatedAt(tutor.getUpdatedAt());
        dto.setLastLoginAt(tutor.getLastLoginAt());
        dto.setChampsLiked(tutor.getChampsLiked());
        dto.setTotalStudents(tutor.getTotalStudents());
        dto.setRating(tutor.getRating());
        dto.setTotalReviews(tutor.getTotalReviews());

        // Direct column fields — always populated
        dto.setFees(tutor.getFees());
        dto.setFeeType(tutor.getFeeType() != null ? tutor.getFeeType().name() : null);
        dto.setStartTime(formatTime(tutor.getStartTime()));
        dto.setEndTime(formatTime(tutor.getEndTime()));
        dto.setTimePreference(tutor.getTimePreference());
        dto.setProfilePicUrl(tutor.getProfilePicUrl());
        dto.setDocumentsVerificationUrl(tutor.getDocumentsVerificationUrl());
        dto.setIsActive(tutor.isActive());
        dto.setIsVerified(tutor.isVerified());

        // Lazy element-collection fields — require @Transactional on caller
        dto.setLevels(tutor.getLevels() != null ? tutor.getLevels() : new ArrayList<>());
        dto.setContactModes(tutor.getContactModes() != null ? tutor.getContactModes() : new ArrayList<>());
        dto.setAvailability(tutor.getAvailability() != null ? tutor.getAvailability() : new ArrayList<>());
        dto.setLanguages(tutor.getLanguages() != null ? tutor.getLanguages() : new ArrayList<>());
        dto.setBoards(tutor.getBoards() != null ? tutor.getBoards() : new ArrayList<>());
        dto.setClasses(tutor.getClasses() != null ? tutor.getClasses() : new ArrayList<>());
        dto.setDegrees(tutor.getDegrees() != null ? tutor.getDegrees() : new ArrayList<>());
        dto.setYears(tutor.getYears() != null ? tutor.getYears() : new ArrayList<>());
        dto.setLanguagesOffered(tutor.getLanguagesOffered() != null ? tutor.getLanguagesOffered() : new ArrayList<>());
        dto.setProficiencyLevels(tutor.getProficiencyLevels() != null ? tutor.getProficiencyLevels() : new ArrayList<>());
        dto.setSkills(tutor.getSkills() != null ? tutor.getSkills() : new ArrayList<>());
        dto.setHobbyProficiency(tutor.getHobbyProficiency() != null ? tutor.getHobbyProficiency() : new ArrayList<>());
        dto.setAgeGroups(tutor.getAgeGroups() != null ? tutor.getAgeGroups() : new ArrayList<>());
        dto.setTargetExams(tutor.getTargetExams() != null ? tutor.getTargetExams() : new ArrayList<>());
        dto.setActivities(tutor.getActivities() != null ? tutor.getActivities() : new ArrayList<>());
        dto.setOtherSkills(tutor.getOtherSkills() != null ? tutor.getOtherSkills() : new ArrayList<>());
        dto.setOtherLevels(tutor.getOtherLevels() != null ? tutor.getOtherLevels() : new ArrayList<>());

        // Category from lazy @OneToMany
        if (tutor.getCategoryMappings() != null && !tutor.getCategoryMappings().isEmpty()) {
            Integer categoryId = tutor.getCategoryMappings().getFirst().getCategoryId();
            dto.setCategory(categoryId != null ? getCategoryNameById(categoryId.longValue()) : null);
        } else {
            dto.setCategory(null);
        }

        return dto;
    }

    private String exportToCsv(List<Tutor> tutors) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,Headline,Address,State,City,Pincode,Age,Gender,Experience,Qualification,Phone,Email,Location,HourlyRate,Status\n");

        for (Tutor tutor : tutors) {
            csv.append(tutor.getId()).append(",")
                    .append(escapeCsv(tutor.getName())).append(",")
                    .append(escapeCsv(tutor.getHeadline())).append(",")
                    .append(escapeCsv(tutor.getAddressLine())).append(",")
                    .append(escapeCsv(tutor.getState())).append(",")
                    .append(escapeCsv(tutor.getCity())).append(",")
                    .append(escapeCsv(tutor.getPincode())).append(",")
                    .append(tutor.getAge()).append(",")
                    .append(tutor.getGender()).append(",")
                    .append(tutor.getExperience()).append(",")
                    .append(escapeCsv(tutor.getQualification())).append(",")
                    .append(escapeCsv(tutor.getPhone())).append(",")
                    .append(escapeCsv(tutor.getEmail())).append(",")
                    .append(escapeCsv(tutor.getLocation())).append(",")
                    .append(tutor.getHourlyRate()).append(",")
                    .append(tutor.isActive() ? "Active" : "Inactive").append("\n");
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String exportToJson(List<Tutor> tutors) {
        try {
            return new ObjectMapper()
                    .writeValueAsString(tutors.stream()
                            .map(this::convertToAdminTutorDTO)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error exporting to JSON", e);
            return "[]";
        }
    }











//    @Autowired
//    private FileStorageService fileStorageService;

    public String uploadProfilePicture(Long tutorId, MultipartFile file) {
        try {
            String relativePath = fileStorageService2.saveProfilePicture(tutorId, file);
            String fullUrl = fileStorageService2.getFullUrl(relativePath);

            // Update tutor with profile picture URL
            tutorRepository.findById(tutorId).ifPresent(tutor -> {
                tutor.setProfilePicUrl(fullUrl);
                tutor.setUpdatedAt(LocalDateTime.now());
                tutorRepository.save(tutor);
            });

            return fullUrl;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile picture: " + e.getMessage(), e);
        }
    }

    public String uploadVerificationDocuments(Long tutorId, MultipartFile file) {
        try {
            String relativePath = fileStorageService2.saveDocument(tutorId, file);
            String fullUrl = fileStorageService2.getFullUrl(relativePath);

            // Update tutor with document URL
            tutorRepository.findById(tutorId).ifPresent(tutor -> {
                String existingDocs = tutor.getDocumentsVerificationUrl();
                String newDocs = (existingDocs != null && !existingDocs.isEmpty())
                        ? existingDocs + "," + fullUrl
                        : fullUrl;
                tutor.setDocumentsVerificationUrl(newDocs);
                tutor.setUpdatedAt(LocalDateTime.now());
                tutorRepository.save(tutor);
            });

            return fullUrl;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }




































    @Transactional
    public AdminTutorDTO getTutorById(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor not found with id: " + id));
        return convertToAdminTutorDTO2(tutor);
    }

//    // Update the convertToAdminTutorDTO method to include all fields
//    private AdminTutorDTO convertToAdminTutorDTO2(Tutor tutor) {
//        AdminTutorDTO dto = new AdminTutorDTO();
//        dto.setId(tutor.getId());
//        dto.setName(tutor.getName());
//        dto.setHeadline(tutor.getHeadline());
//        dto.setAge(tutor.getAge());
//        dto.setGender(tutor.getGender() != null ? tutor.getGender().getDisplayName() : null);
//        dto.setExperience(tutor.getExperience() != null ? tutor.getExperience().getDisplayName() : null);
//        dto.setQualification(tutor.getQualification());
//        dto.setPhone(tutor.getPhone());
//        dto.setEmail(tutor.getEmail());
//        dto.setDocumentsVerified(tutor.isVerified());
//        dto.setHourlyRate(tutor.getHourlyRate() != null ? tutor.getHourlyRate().doubleValue() : null);
//        dto.setLocation(tutor.getLocation());
//        dto.setAddressLine(tutor.getAddressLine());
//        dto.setState(tutor.getState());
//        dto.setCity(tutor.getCity());
//        dto.setPincode(tutor.getPincode());
//        dto.setSubjects(tutor.getSubjects() != null
//                ? tutor.getSubjects().stream().map(Tutor.Subject::getDisplayName).collect(Collectors.toList())
//                : null);
//        dto.setEntityType(tutor.getEntityType() != null ? tutor.getEntityType().getDisplayName() : null);
//        dto.setEntityName(tutor.getEntityName());
//        dto.setStatus(tutor.isActive() ? "Active" : "Inactive");
//        dto.setVerificationStatus(tutor.isVerified() ? "Verified" : "Pending");
//        dto.setCreatedAt(tutor.getCreatedAt());
//        dto.setUpdatedAt(tutor.getUpdatedAt());
//        dto.setLastLoginAt(tutor.getLastLoginAt());
//        dto.setChampsLiked(tutor.getChampsLiked());
//        dto.setTotalStudents(tutor.getTotalStudents());
//        dto.setRating(tutor.getRating());
//        dto.setTotalReviews(tutor.getTotalReviews());
//
//        // Additional fields for edit form
//        dto.setFees(tutor.getFees());
//        dto.setFeeType(tutor.getFeeType() != null ? tutor.getFeeType().name() : null);
//        dto.setStartTime(tutor.getStartTime());
//        dto.setEndTime(tutor.getEndTime());
//        dto.setLevels(tutor.getLevels());
//        dto.setContactModes(tutor.getContactModes());
//        dto.setAvailability(tutor.getAvailability());
//        dto.setProfilePicUrl(tutor.getProfilePicUrl());
//        dto.setDocumentsVerificationUrl(tutor.getDocumentsVerificationUrl());
//        dto.setIsActive(tutor.isActive());
//        dto.setIsVerified(tutor.isVerified());
//
//        return dto;
//    }



    private AdminTutorDTO convertToAdminTutorDTO2(Tutor tutor) {
        AdminTutorDTO dto = new AdminTutorDTO();
        dto.setId(tutor.getId());
        dto.setName(tutor.getName());
        dto.setHeadline(tutor.getHeadline());
        dto.setAge(tutor.getAge());
        dto.setGender(tutor.getGender() != null ? tutor.getGender().name().toLowerCase() : null);
        // Return display name (e.g. "3-5 Years") so the edit form dropdown can pre-select it
        dto.setExperience(tutor.getExperience() != null ? tutor.getExperience().getDisplayName() : null);
        dto.setQualification(tutor.getQualification());
        dto.setPhone(tutor.getPhone());
        dto.setEmail(tutor.getEmail());
        dto.setDocumentsVerified(tutor.isVerified());
        dto.setHourlyRate(tutor.getHourlyRate() != null ? tutor.getHourlyRate().doubleValue() : null);
        dto.setLocation(tutor.getLocation());
        dto.setAddressLine(tutor.getAddressLine());
        dto.setState(tutor.getState());
        dto.setCity(tutor.getCity());
        dto.setPincode(tutor.getPincode());
        // Return lowercase enum names ("mathematics") — matches what the create/update form sends
        dto.setSubjects(tutor.getSubjects() != null
                ? tutor.getSubjects().stream()
                        .map(s -> s.name().toLowerCase())
                        .collect(Collectors.toList())
                : new ArrayList<>());
        // Return uppercase enum name ("INDIVIDUAL", "SCHOOL") — what the update form sends after toUpperCase()
        dto.setEntityType(tutor.getEntityType() != null ? tutor.getEntityType().name() : null);
        dto.setEntityName(tutor.getEntityName());
        dto.setStatus(tutor.isActive() ? "Active" : "Inactive");
        dto.setVerificationStatus(tutor.isVerified() ? "Verified" : "Pending");
        dto.setCreatedAt(tutor.getCreatedAt());
        dto.setUpdatedAt(tutor.getUpdatedAt());
        dto.setLastLoginAt(tutor.getLastLoginAt());
        dto.setChampsLiked(tutor.getChampsLiked());
        dto.setTotalStudents(tutor.getTotalStudents());
        dto.setRating(tutor.getRating());
        dto.setTotalReviews(tutor.getTotalReviews());

        // Edit-form fields
        dto.setFees(tutor.getFees());
        dto.setFeeType(tutor.getFeeType() != null ? tutor.getFeeType().name() : null);
        dto.setStartTime(formatTime(tutor.getStartTime()));
        dto.setEndTime(formatTime(tutor.getEndTime()));
        dto.setTimePreference(tutor.getTimePreference());
        dto.setLevels(tutor.getLevels() != null ? tutor.getLevels() : new ArrayList<>());
        dto.setContactModes(tutor.getContactModes() != null ? tutor.getContactModes() : new ArrayList<>());
        dto.setAvailability(tutor.getAvailability() != null ? tutor.getAvailability() : new ArrayList<>());
        dto.setLanguages(tutor.getLanguages() != null ? tutor.getLanguages() : new ArrayList<>());
        dto.setBoards(tutor.getBoards() != null ? tutor.getBoards() : new ArrayList<>());
        dto.setClasses(tutor.getClasses() != null ? tutor.getClasses() : new ArrayList<>());
        dto.setDegrees(tutor.getDegrees() != null ? tutor.getDegrees() : new ArrayList<>());
        dto.setYears(tutor.getYears() != null ? tutor.getYears() : new ArrayList<>());
        dto.setLanguagesOffered(tutor.getLanguagesOffered() != null ? tutor.getLanguagesOffered() : new ArrayList<>());
        dto.setProficiencyLevels(tutor.getProficiencyLevels() != null ? tutor.getProficiencyLevels() : new ArrayList<>());
        dto.setSkills(tutor.getSkills() != null ? tutor.getSkills() : new ArrayList<>());
        dto.setHobbyProficiency(tutor.getHobbyProficiency() != null ? tutor.getHobbyProficiency() : new ArrayList<>());
        dto.setAgeGroups(tutor.getAgeGroups() != null ? tutor.getAgeGroups() : new ArrayList<>());
        dto.setTargetExams(tutor.getTargetExams() != null ? tutor.getTargetExams() : new ArrayList<>());
        dto.setActivities(tutor.getActivities() != null ? tutor.getActivities() : new ArrayList<>());
        dto.setOtherSkills(tutor.getOtherSkills() != null ? tutor.getOtherSkills() : new ArrayList<>());
        dto.setOtherLevels(tutor.getOtherLevels() != null ? tutor.getOtherLevels() : new ArrayList<>());
        dto.setProfilePicUrl(tutor.getProfilePicUrl());
        dto.setDocumentsVerificationUrl(tutor.getDocumentsVerificationUrl());
        dto.setIsActive(tutor.isActive());
        dto.setIsVerified(tutor.isVerified());

        if (tutor.getCategoryMappings() != null && !tutor.getCategoryMappings().isEmpty()) {
            Integer categoryId = tutor.getCategoryMappings().getFirst().getCategoryId();
            dto.setCategory(categoryId != null ? getCategoryNameById(categoryId.longValue()) : null);
        } else {
            dto.setCategory(null);
        }





        // ✅ ADD THIS - Populate categories from tutor_category_mappings
//        if (tutor.getCategoryMappings() != null && !tutor.getCategoryMappings().isEmpty()) {
//            List<TutorCategoryMappingDto> categoryDtos = new ArrayList<>();
//
//            for (TutorCategoryMapping mapping : tutor.getCategoryMappings()) {
//                TutorCategoryMappingDto categoryDto = new TutorCategoryMappingDto();
//                categoryDto.setCategoryId(mapping.getId());
//                categoryDto.setCategoryId(mapping.getCategoryId());
//                categoryDto.setExpertiseLevel(mapping.getExpertiseLevel());
//                categoryDto.setYearsOfExperience(mapping.getYearsOfExperience());
//                categoryDto.setIsAcceptingStudents(mapping.getIsAcceptingStudents());
//                categoryDto.setMaxStudentsPerBatch(mapping.getMaxStudentsPerBatch());
//                categoryDto.setCategoryHourlyRate(mapping.getCategoryHourlyRate());
//                categoryDto.setCategoryBatchRate(mapping.getCategoryBatchRate());
//                categoryDto.setDescription(mapping.getDescription());
////                categoryDto.setStudentCount(mapping.getStudentCount());
////                categoryDto.setTotalReviews(mapping.getTotalReviews());
////                categoryDto.setCategoryRating(mapping.getCategoryRating());
////                categoryDto.setClassesCompleted(mapping.getClassesCompleted());
////                categoryDto.setCoursesCompleted(mapping.getCoursesCompleted());
////                categoryDto.setCurrentStudents(mapping.getCurrentStudents());
////                categoryDto.setLastClassTaught(mapping.getLastClassTaught());
//
//
//
//                categoryDtos.add(categoryDto);
//            }
//            dto.setCategories(categoryDtos);
//        }

        return dto;
    }

    private String getCategoryNameById(Long categoryId){
        // You need to inject CategoryRepository or use a map
        // Option 1: If you have CategoryRepository
        // return categoryRepository.findById(categoryId)
        //     .map(Category::getCategoryName)
        //     .orElse(null);

        // Option 2: Simple mapping based on your data
        Map<Long, String> categoryMap = Map.of(
                1L, "School",
                2L, "College",
                3L, "Languages",
                4L, "Hobbies",
                5L, "Exams",
                6L, "Sports",
                7L, "Others"
        );

        return categoryMap.get(categoryId);
    }




}
