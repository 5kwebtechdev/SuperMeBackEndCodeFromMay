package com.superme.admin.controller;

import com.superme.dto.AdminAdDTO;
import com.superme.dto.AdminAdOverviewResponseDTO;
import com.superme.dto.AdStatistics;
import com.superme.enums.AdStatus;
import com.superme.enums.AdType;
import com.superme.enums.TargetScreen;
import com.superme.service.AdminAdService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/ads")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminAdController {

    private final AdminAdService adminAdService;

    @GetMapping("/overview")
    public ResponseEntity<AdminAdOverviewResponseDTO> getAdminAdOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) List<AdType> filterAdTypes,
            @RequestParam(required = false) List<TargetScreen> filterTargetScreens,
            @RequestParam(required = false) List<AdStatus> filterStatuses) {

        AdminAdOverviewResponseDTO response = adminAdService.getAdminAdOverview(
                page, size, sortBy, sortDir, searchText, filterAdTypes, filterTargetScreens, filterStatuses);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/datatable")
    public ResponseEntity<Page<AdminAdDTO>> getAdDataTable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) List<AdType> filterAdTypes,
            @RequestParam(required = false) List<TargetScreen> filterTargetScreens,
            @RequestParam(required = false) List<AdStatus> filterStatuses) {

        Page<AdminAdDTO> ads = adminAdService.getAdDataTable(
                page, size, sortBy, sortDir, searchText, filterAdTypes, filterTargetScreens, filterStatuses);

        return ResponseEntity.ok(ads);
    }

    @GetMapping("/stats")
    public ResponseEntity<AdStatistics> getAdStats() {
        AdStatistics stats = adminAdService.getAdStatsForCards();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/filter-options")
    public ResponseEntity<AdminAdOverviewResponseDTO.FilterOptions> getFilterOptions() {
        AdminAdOverviewResponseDTO.FilterOptions options = adminAdService.getFilterOptions();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/search-suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(@RequestParam String query) {
        List<String> suggestions = adminAdService.getSearchSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping
    public ResponseEntity<AdminAdDTO> createAd(@RequestBody AdminAdDTO ad) {
        try {
            AdminAdDTO createdAd = adminAdService.createAd(ad);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAd);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminAdDTO> getAdById(@PathVariable Long id) {
        Optional<AdminAdDTO> ad = adminAdService.getAdById(id);
        return ad.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminAdDTO> updateAd(@PathVariable Long id, @RequestBody AdminAdDTO adDetails) {
        try {
            AdminAdDTO updatedAd = adminAdService.updateAd(id, adDetails);
            return ResponseEntity.ok(updatedAd);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAd(@PathVariable Long id) {
        try {
            adminAdService.deleteAd(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminAdDTO> updateAdStatus(@PathVariable Long id, @RequestParam AdStatus status) {
        try {
            AdminAdDTO updatedAd = adminAdService.updateAdStatus(id, status);
            return ResponseEntity.ok(updatedAd);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/bulk-create")
    public ResponseEntity<List<AdminAdDTO>> createBulkAds(@RequestBody List<AdminAdDTO> ads) {
        try {
            List<AdminAdDTO> createdAds = ads.stream()
                    .map(adminAdService::createAd)
                    .toList();
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAds);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/bulk-delete")
    public ResponseEntity<Void> deleteBulkAds(@RequestBody List<Long> ids) {
        try {
            ids.forEach(adminAdService::deleteAd);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/validate-name")
    public ResponseEntity<Map<String, Boolean>> validateAdName(@RequestParam String adName) {
        boolean isUnique = adminAdService.getAllAds().stream()
                .noneMatch(ad -> ad.getAdName().equalsIgnoreCase(adName));
        Map<String, Boolean> response = Map.of("isUnique", isUnique);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        Map<String, String> error = Map.of("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        Map<String, String> error = Map.of("error", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}