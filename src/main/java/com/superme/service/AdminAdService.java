package com.superme.service;

import com.superme.dto.AdminAdDTO;
import com.superme.dto.AdminAdOverviewResponseDTO;
import com.superme.dto.AdStatistics;
import com.superme.model.Ad;
import com.superme.enums.*;
import com.superme.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAdService {

    private final AdRepository adRepository;

    public AdminAdOverviewResponseDTO getAdminAdOverview(
            int page, int size, String sortBy, String sortDir,
            String searchText, List<AdType> filterAdTypes,
            List<TargetScreen> filterTargetScreens, List<AdStatus> filterStatuses) {

        Page<AdminAdDTO> dataTable = getAdDataTable(
                page, size, sortBy, sortDir, searchText, filterAdTypes, filterTargetScreens, filterStatuses);

        AdStatistics stats = getAdStatsForCards();
        AdminAdOverviewResponseDTO.FilterOptions filterOptions = getFilterOptions();

        AdminAdOverviewResponseDTO response = new AdminAdOverviewResponseDTO();
        response.setAds(dataTable.getContent());
        response.setStatistics(stats);
        response.setFilterOptions(filterOptions);
        response.setTotalElements(dataTable.getTotalElements());
        response.setTotalPages(dataTable.getTotalPages());
        response.setCurrentPage(dataTable.getNumber());
        response.setPageSize(dataTable.getSize());
        response.setHasNext(dataTable.hasNext());
        response.setHasPrevious(dataTable.hasPrevious());

        return response;
    }

    public Page<AdminAdDTO> getAdDataTable(
            int page, int size, String sortBy, String sortDir,
            String searchText, List<AdType> filterAdTypes,
            List<TargetScreen> filterTargetScreens, List<AdStatus> filterStatuses) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Ad> adPage = adRepository.findAll(pageable);

        List<AdminAdDTO> filteredAds = adPage.getContent().stream()
                .filter(ad -> applySearchCriteria(ad, searchText))
                .filter(ad -> applyFilterCriteria(ad, filterAdTypes, filterTargetScreens, filterStatuses))
                .map(this::convertToAdminAdDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(filteredAds, pageable, adPage.getTotalElements());
    }

    public AdStatistics getAdStatsForCards() {
        long totalAds = adRepository.count();
        long activeAds = adRepository.countByStatus(AdStatus.ACTIVE);
        long inactiveAds = totalAds - activeAds;

        return new AdStatistics(totalAds, activeAds, inactiveAds);
    }

    public List<String> getSearchSuggestions(String query) {
        List<String> suggestions = Arrays.asList();

        if (query != null && !query.trim().isEmpty()) {
            String searchQuery = query.toLowerCase();

            List<String> nameSuggestions = adRepository.findAll().stream()
                    .map(Ad::getAdName)
                    .filter(name -> name != null && name.toLowerCase().contains(searchQuery))
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());

            suggestions = nameSuggestions;
        }

        return suggestions.stream().distinct().limit(10).collect(Collectors.toList());
    }

    public AdminAdOverviewResponseDTO.FilterOptions getFilterOptions() {
        AdminAdOverviewResponseDTO.FilterOptions options = new AdminAdOverviewResponseDTO.FilterOptions();

        options.setAdTypes(Arrays.stream(AdType.values())
                .map(Enum::name)
                .collect(Collectors.toList()));

        options.setTargetScreens(Arrays.stream(TargetScreen.values())
                .map(Enum::name)
                .collect(Collectors.toList()));

        options.setAgeGroups(Arrays.stream(AgeGroup.values())
                .map(Enum::name)
                .collect(Collectors.toList()));

        options.setGenders(Arrays.stream(Gender.values())
                .map(Enum::name)
                .collect(Collectors.toList()));

        options.setPriorities(Arrays.stream(Priority.values())
                .map(Enum::name)
                .collect(Collectors.toList()));

        options.setStatuses(Arrays.stream(AdStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList()));

        return options;
    }

    @Transactional
    public AdminAdDTO createAd(AdminAdDTO dto) {
        Ad ad = convertToEntity(dto);
        ad.setCreatedAt(LocalDateTime.now());
        ad.setUpdatedAt(LocalDateTime.now());

        Ad saved = adRepository.save(ad);
        return convertToAdminAdDTO(saved);
    }

    public Optional<AdminAdDTO> getAdById(Long id) {
        return adRepository.findById(id)
                .map(this::convertToAdminAdDTO);
    }

    @Transactional
    public AdminAdDTO updateAd(Long id, AdminAdDTO adDetails) {
        Ad existingAd = adRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ad not found with id: " + id));

        updateEntityFromDTO(existingAd, adDetails);
        existingAd.setUpdatedAt(LocalDateTime.now());

        Ad updated = adRepository.save(existingAd);
        return convertToAdminAdDTO(updated);
    }

    @Transactional
    public void deleteAd(Long id) {
        if (!adRepository.existsById(id)) {
            throw new RuntimeException("Ad not found with id: " + id);
        }
        adRepository.deleteById(id);
    }

    @Transactional
    public AdminAdDTO updateAdStatus(Long id, AdStatus status) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ad not found with id: " + id));

        ad.setStatus(status);
        ad.setUpdatedAt(LocalDateTime.now());

        Ad updated = adRepository.save(ad);
        return convertToAdminAdDTO(updated);
    }

    private boolean applySearchCriteria(Ad ad, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return true;
        }

        String searchLower = searchText.toLowerCase();

        if (ad.getId() != null && ad.getId().toString().contains(searchText)) {
            return true;
        }

        if (ad.getAdName() != null && ad.getAdName().toLowerCase().contains(searchLower)) {
            return true;
        }

        if (ad.getAdDescription() != null && ad.getAdDescription().toLowerCase().contains(searchLower)) {
            return true;
        }

        return false;
    }

    private boolean applyFilterCriteria(Ad ad, List<AdType> filterAdTypes,
                                        List<TargetScreen> filterTargetScreens,
                                        List<AdStatus> filterStatuses) {

        if (filterAdTypes != null && !filterAdTypes.isEmpty() && !filterAdTypes.contains(ad.getAdType())) {
            return false;
        }

        if (filterTargetScreens != null && !filterTargetScreens.isEmpty() && !filterTargetScreens.contains(ad.getTargetScreen())) {
            return false;
        }

        if (filterStatuses != null && !filterStatuses.isEmpty() && !filterStatuses.contains(ad.getStatus())) {
            return false;
        }

        return true;
    }

    private AdminAdDTO convertToAdminAdDTO(Ad ad) {
        AdminAdDTO dto = new AdminAdDTO();
        dto.setId(ad.getId());
        dto.setAdName(ad.getAdName());
        dto.setAdDescription(ad.getAdDescription());
        dto.setAdType(ad.getAdType());
        dto.setBannerImageS3Key(ad.getBannerImageS3Key());
        dto.setBannerImageUrl(ad.getBannerImageUrl());
        dto.setClickActionType(ad.getClickActionType());
        dto.setTargetScreen(ad.getTargetScreen());
        dto.setTargetAudience(ad.getTargetAudience());
        dto.setGender(ad.getGender());
        dto.setExternalUrl(ad.getExternalUrl());
        dto.setInAppScreen(ad.getInAppScreen());
        dto.setStartDateTime(ad.getStartDateTime());
        dto.setEndDateTime(ad.getEndDateTime());
        dto.setNoEndDate(ad.getNoEndDate());
        dto.setPriority(ad.getPriority());
        dto.setStatus(ad.getStatus());
        dto.setCreatedAt(ad.getCreatedAt());
        dto.setUpdatedAt(ad.getUpdatedAt());
        return dto;
    }

    private Ad convertToEntity(AdminAdDTO dto) {
        Ad ad = new Ad();
        updateEntityFromDTO(ad, dto);
        return ad;
    }

    private void updateEntityFromDTO(Ad ad, AdminAdDTO dto) {
        ad.setAdName(dto.getAdName());
        ad.setAdDescription(dto.getAdDescription());
        ad.setAdType(dto.getAdType());
        ad.setBannerImageS3Key(dto.getBannerImageS3Key());
        ad.setBannerImageUrl(dto.getBannerImageUrl());
        ad.setClickActionType(dto.getClickActionType());
        ad.setTargetScreen(dto.getTargetScreen());
        ad.setTargetAudience(dto.getTargetAudience());
        ad.setGender(dto.getGender());
        ad.setExternalUrl(dto.getExternalUrl());
        ad.setInAppScreen(dto.getInAppScreen());
        ad.setStartDateTime(dto.getStartDateTime());
        ad.setEndDateTime(dto.getEndDateTime());
        ad.setNoEndDate(dto.getNoEndDate());
        ad.setPriority(dto.getPriority());
        ad.setStatus(dto.getStatus());
    }

    public List<Ad> getAllAds() {
        return adRepository.findAll();
    }

    public Page<Ad> getAllAds(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return adRepository.findAll(pageable);
    }
}