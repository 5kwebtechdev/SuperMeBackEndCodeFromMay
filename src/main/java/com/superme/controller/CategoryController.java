package com.superme.controller;

import com.superme.dto.CategoryIconRequest;
import com.superme.dto.CategoryRequest;
import com.superme.exception.BusinessException;
import com.superme.service.CategoryService;
import com.superme.dto.CategoryIconUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/categories")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class CategoryController {

    /** Base path for icons stored on S3, e.g. journal/emotions/happy.png */
    private static final String S3_BASE_URL = "https://your-bucket.s3.amazonaws.com/journal/";

    @Autowired
    private CategoryService categoryService;

    // In-memory storage for categories and icons (replace with DB/service in production)
      final static Map<String, List<Map<String, String>>> options = new HashMap<>();

    public CategoryController() {
        // Initialize with default categories and icons
        options.put("emotions", new ArrayList<>(List.of(
                icon("happy", "emotions/happy.png"),
                icon("excited", "emotions/excited.png"),
                icon("angry", "emotions/angry.png"),
                icon("sad", "emotions/sad.png")
        )));
        options.put("sleep", new ArrayList<>(List.of(
                icon("good", "sleep/good.png"),
                icon("bad", "sleep/bad.png"),
                icon("medium", "sleep/medium.png"),
                icon("early", "sleep/early.png")
        )));
        options.put("health", new ArrayList<>(List.of(
                icon("good", "health/good.png"),
                icon("bad", "health/bad.png"),
                icon("sick", "health/sick.png"),
                icon("healthy", "health/healthy.png")
        )));
        options.put("hobbies", new ArrayList<>(List.of(
                icon("reading", "hobbies/reading.png"),
                icon("sports", "hobbies/sports.png"),
                icon("music", "hobbies/music.png"),
                icon("travel", "hobbies/travel.png")
        )));
        options.put("food", new ArrayList<>(List.of(
                icon("healthy", "food/healthy.png"),
                icon("unhealthy", "food/unhealthy.png"),
                icon("homecooked", "food/homecooked.png"),
                icon("fastfood", "food/fastfood.png")
        )));
        options.put("social", new ArrayList<>(List.of(
                icon("friends", "social/friends.png"),
                icon("family", "social/family.png"),
                icon("alone", "social/alone.png"),
                icon("party", "social/party.png")
        )));
        options.put("school", new ArrayList<>(List.of(
                icon("good", "school/good.png"),
                icon("bad", "school/bad.png"),
                icon("study", "school/study.png"),
                icon("exam", "school/exam.png")
        )));
    }

    @GetMapping("/options")
    public Map<String, List<Map<String, String>>> getCategoryOptions() {
        return options;
    }

    // --- ADMIN ENDPOINTS ---

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<String> addCategoryIcon(@RequestBody CategoryIconRequest req) {
        System.out.println("hlo1");
        options.putIfAbsent(req.getCategory(), new ArrayList<>());
        options.get(req.getCategory()).add(icon(req.getValue(), req.getIconPath()));
        return ResponseEntity.status(HttpStatus.CREATED).body("Icon added to category: " + req.getCategory());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteCategoryIcon(@RequestBody CategoryIconRequest req) {
        List<Map<String, String>> icons = options.get(req.getCategory());
        if (icons != null) {
            boolean removed = icons.removeIf(icon -> icon.get("value").equals(req.getValue()));
            if (removed) {
                return ResponseEntity.ok("Icon removed from category: " + req.getCategory());
            }
        }
        throw new BusinessException("Icon not found in category: " + req.getCategory());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete-category")
    public ResponseEntity<String> deleteCategory(@RequestBody CategoryRequest req) {
        if (options.remove(req.getCategory()) != null) {
            return ResponseEntity.ok("Category deleted: " + req.getCategory());
        }
        throw new BusinessException("Category not found: " + req.getCategory());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/update")
    public ResponseEntity<String> updateCategoryIcon(@RequestBody CategoryIconUpdateRequest req) {
        List<Map<String, String>> icons = options.get(req.getCategory());
        if (icons != null) {
//            for (Map<String, String> icon : icons) {
//                if (icon.get("value").equals(req.getOldValue())) {
//                    icon.put("value", req.getNewValue());
//                    icon.put("icon", S3_BASE_URL + req.getNewIconPath());
//                    return ResponseEntity.ok("Icon updated in category: " + req.getCategory());
//                }
//            }
            for (int i = 0; i < icons.size(); i++) {
                Map<String, String> icon = new HashMap<>(icons.get(i)); // ✅ mutable copy

                if (icon.get("value").equalsIgnoreCase(req.getOldValue())) {
                    icon.put("value", req.getNewValue());
                    icon.put("icon", S3_BASE_URL + req.getNewIconPath());
                    icons.set(i, icon); // ✅ replace old immutable map
                    return ResponseEntity.ok("Updated");
                }
            }
        }
       throw new BusinessException("Icon not found in category: " + req.getCategory());
    }

    /* -------------------------------------------------------------------- */
    /* Utility to build a small JSON object                                 */
    /* -------------------------------------------------------------------- */
    private static Map<String, String> icon(String value, String key) {
        return Map.of(
                "value", value,
                "icon",  S3_BASE_URL + key
        );
    }
}
