package com.superme.service;

import com.superme.exception.ResourceNotFoundException;
import com.superme.model.CategoryField;
import com.superme.model.FieldOption;
import com.superme.repository.CategoryFieldRepository;
import com.superme.repository.FieldOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FieldOptionService {

    private final FieldOptionRepository optionRepository;
    private final CategoryFieldRepository fieldRepository;

    /**
     * Get all options for a field
     */
    public List<FieldOption> getOptionsByField(Long fieldId) {
        log.info("Fetching options for field: {}", fieldId);
        validateFieldExists(fieldId);
        return optionRepository.findByFieldIdOrderByDisplayOrder(fieldId);
    }

    /**
     * Get option by ID
     */
    public FieldOption getOptionById(Long optionId) {
        log.info("Fetching option with id: {}", optionId);
        return optionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Option not found with id: " + optionId));
    }

    /**
     * Create new option
     */
    public FieldOption createOption(Long fieldId, FieldOption option) {
        log.info("Creating new option for field: {}", fieldId);

        CategoryField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found with id: " + fieldId));

        // Check duplicate option value
        if (optionRepository.findByFieldIdAndOptionValue(fieldId, option.getOptionValue()).isPresent()) {
            throw new RuntimeException("Option '" + option.getOptionValue() + "' already exists for this field");
        }

        option.setField(field);
        option.setIsActive(true);
        FieldOption saved = optionRepository.save(option);
        log.info("Option created successfully with id: {}", saved.getId());
        return saved;
    }

    /**
     * Update option
     */
    public FieldOption updateOption(Long optionId, FieldOption optionDetails) {
        log.info("Updating option with id: {}", optionId);

        FieldOption option = getOptionById(optionId);

        // Check if value is being changed
        if (!option.getOptionValue().equals(optionDetails.getOptionValue())) {
            if (optionRepository.findByFieldIdAndOptionValue(option.getField().getId(), optionDetails.getOptionValue()).isPresent()) {
                throw new RuntimeException("Option '" + optionDetails.getOptionValue() + "' already exists for this field");
            }
        }

        option.setOptionValue(optionDetails.getOptionValue());
        option.setOptionLabel(optionDetails.getOptionLabel());
        option.setDisplayOrder(optionDetails.getDisplayOrder());
        option.setIsActive(optionDetails.getIsActive());

        FieldOption updated = optionRepository.save(option);
        log.info("Option updated successfully with id: {}", optionId);
        return updated;
    }

    /**
     * Delete option
     */
    public void deleteOption(Long optionId) {
        log.info("Deleting option with id: {}", optionId);

        FieldOption option = getOptionById(optionId);
        optionRepository.delete(option);

        log.info("Option deleted successfully with id: {}", optionId);
    }

    /**
     * Bulk create options
     */
    public List<FieldOption> bulkCreateOptions(Long fieldId, List<FieldOption> options) {
        log.info("Creating {} options for field: {}", options.size(), fieldId);

        CategoryField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found with id: " + fieldId));

        List<FieldOption> optionsToSave = options.stream()
                .peek(opt -> {
                    opt.setField(field);
                    opt.setIsActive(true);
                })
                .toList();

        List<FieldOption> saved = optionRepository.saveAll(optionsToSave);
        log.info("Bulk options created successfully");
        return saved;
    }

    private void validateFieldExists(Long fieldId) {
        if (!fieldRepository.existsById(fieldId)) {
            throw new RuntimeException("Field not found with id: " + fieldId);
        }
    }
}
