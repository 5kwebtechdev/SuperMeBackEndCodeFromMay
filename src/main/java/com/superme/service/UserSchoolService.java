package com.superme.service;

import com.superme.dto.ClassListItemDto;
import com.superme.dto.SchoolListItemDto;
import com.superme.model.School;
import com.superme.model.SchoolClass;
import com.superme.repository.SchoolClassRepository;
import com.superme.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserSchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;

    public Page<SchoolListItemDto> listSchools(String search, Pageable pageable) {
        Page<School> page;
        if (search != null && !search.trim().isEmpty()) {
            page = schoolRepository
                    .findBySchoolNameContainingIgnoreCaseOrSchoolBranchContainingIgnoreCaseOrCityContainingIgnoreCase(
                            search, search, search, pageable);
        } else {
            page = schoolRepository.findAll(pageable);
        }
        return page.map(this::toSchoolDto);
    }

    public List<ClassListItemDto> listClasses(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found: " + schoolId));
        List<SchoolClass> classes = schoolClassRepository.findBySchool(school);

        return classes.stream()
                .map(c -> ClassListItemDto.builder()
                        .name(List.of(c.getName()))
                        .build())
                .toList();
    }

    public void saveUserSelection(Long schoolId, List<String> className) {
        // no-op (or just log); selection is handled on client side or in another service
        log.info("User selected schoolId={} className={}", schoolId, className);
    }

    private SchoolListItemDto toSchoolDto(School s) {
        return SchoolListItemDto.builder()
                .id(s.getId())
                .logoUrl(s.getLogo())
                .name(s.getSchoolName())
                .branch(s.getSchoolBranch())
                .city(s.getCity())
                .state(s.getState())
                .build();
    }
}
