package com.superme.service;

import com.superme.dto.SchoolFormRequest;
import com.superme.dto.SchoolListItem;
import com.superme.model.School;
import com.superme.model.SchoolClass;
import com.superme.repository.SchoolRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<SchoolListItem> list() {
        return schoolRepository.findAll()
                .stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SchoolFormRequest getById(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("School not found"));
        return toFormRequest(school);
    }

    @Transactional
    public Long create(SchoolFormRequest request) {
        School school = new School(); // Use no-args constructor
        school.setClasses(new ArrayList<>()); // Initialize mutable list
        mapFromRequest(request, school);

        if (school.getCatalog() == null) {
            school.setCatalog(0);
        }
        if (school.getStatus() == null) {
            school.setStatus("Active");
        }
        return schoolRepository.save(school).getId();
    }

    @Transactional
    public void update(Long id, SchoolFormRequest request) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("School not found"));
        mapFromRequest(request, school);
        schoolRepository.save(school);
    }

    @Transactional
    public void delete(Long id) {
        if (!schoolRepository.existsById(id)) {
            throw new EntityNotFoundException("School not found");
        }
        schoolRepository.deleteById(id);
    }

    private void mapFromRequest(SchoolFormRequest request, School school) {
        school.setLogo(request.getLogo());
        school.setSchoolName(request.getSchoolName());
        school.setSchoolBranch(request.getSchoolBranch());
        school.setCity(request.getCity());
        school.setState(request.getState());
        school.setGenderType(request.getGenderType());
        school.setCatalog(request.getCatalog());
        school.setStatus(request.getStatus());

        // SAFE: classes list is always initialized and mutable
        school.getClasses().clear();
        if (request.getClassName() != null && !request.getClassName().isEmpty()) {
            List<SchoolClass> classes = request.getClassName().stream()
                    .map(name -> SchoolClass.builder()
                            .name(name.trim())
                            .school(school)
                            .build())
                    .collect(Collectors.toList());
            school.getClasses().addAll(classes);
        }
    }

    private SchoolListItem toListItem(School s) {
        List<String> classNames = (s.getClasses() != null && !s.getClasses().isEmpty())
                ? s.getClasses().stream().map(SchoolClass::getName).collect(Collectors.toList())
                : new ArrayList<>();

        return SchoolListItem.builder()
                .id(s.getId())
                .logo(s.getLogo())
                .schoolName(s.getSchoolName())
                .schoolBranch(s.getSchoolBranch())
                .city(s.getCity())
                .state(s.getState())
                .className(classNames)
                .genderType(s.getGenderType())
                .catalog(s.getCatalog())
                .status(s.getStatus())
                .build();
    }

    private SchoolFormRequest toFormRequest(School s) {
        List<String> classNames = (s.getClasses() != null && !s.getClasses().isEmpty())
                ? s.getClasses().stream().map(SchoolClass::getName).collect(Collectors.toList())
                : new ArrayList<>();

        return SchoolFormRequest.builder()
                .logo(s.getLogo())
                .schoolName(s.getSchoolName())
                .schoolBranch(s.getSchoolBranch())
                .city(s.getCity())
                .state(s.getState())
                .className(classNames)
                .genderType(s.getGenderType())
                .catalog(s.getCatalog())
                .status(s.getStatus())
                .build();
    }
}
