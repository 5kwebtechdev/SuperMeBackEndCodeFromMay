package com.superme.controller;


import com.superme.dto.LearningSearchResponse;
import com.superme.enums.Category;
import com.superme.service.LearningSearchService;
import com.superme.util.UserJwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearningSearchController {

    private final LearningSearchService learningSearchService;
    @GetMapping("/search")
    public ResponseEntity<List<LearningSearchResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(
                learningSearchService.search(keyword, category)
        );
    }

}
