package com.superme.controller;

import com.superme.dto.ClassListItemDto;
import com.superme.dto.SchoolListItemDto;
import com.superme.service.UserSchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserSchoolController {

    private final UserSchoolService userSchoolService;

    @GetMapping("/schools")
    public Page<SchoolListItemDto> listSchools(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userSchoolService.listSchools(search, PageRequest.of(page, size));
    }

    @GetMapping("/schools/{schoolId}/classes")
    public List<ClassListItemDto> listClasses(@PathVariable Long schoolId) {
        return userSchoolService.listClasses(schoolId);
    }

    @PostMapping("/selection")
    public void saveSelection(@RequestParam Long schoolId,
                              @RequestParam List<String> className) {
        userSchoolService.saveUserSelection(schoolId, className);
    }
}
