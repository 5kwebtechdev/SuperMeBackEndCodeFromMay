package com.superme.service;

import com.superme.model.FaqMaster;
import com.superme.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    public List<FaqMaster> getFaqs(String screen, String section) {

        // CASE 1: No filters → return all
        if ((screen == null || screen.isEmpty()) &&
                (section == null || section.isEmpty())) {
            return faqRepository.findAll();
        }

        // CASE 2: Filter by screen + section
        if (screen != null && section != null) {
            return faqRepository.findByScreenAndSection(screen, section);
        }

        // CASE 3: Filter by screen only
        if (screen != null) {
            return faqRepository.findByScreen(screen);
        }

        // CASE 4: Filter by section only
        return faqRepository.findBySection(section);
    }
}

