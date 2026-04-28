package com.superme.controller;

import com.superme.model.FaqMaster;
import com.superme.service.FaqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/faq")
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @GetMapping
    public ResponseEntity<List<FaqMaster>> getFaqs(
            @RequestParam(required = false) String screen,
            @RequestParam(required = false) String section
    ) {
        List<FaqMaster> response = faqService.getFaqs(screen, section);
        return ResponseEntity.ok(response);
    }
}
