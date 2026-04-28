package com.superme.service;

import com.superme.dto.FeedbackRequestDto;
import com.superme.model.Feedback;
import com.superme.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService {


    private final FeedbackRepository feedbackRepository;

    public void submitFeedback(Long userId, FeedbackRequestDto request) {

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .rating(request.getRating())
                .appExperience(request.getAppExperience())
                .featureRequest(request.getFeatureRequest())
                .build();

        feedbackRepository.save(feedback);
    }
}
