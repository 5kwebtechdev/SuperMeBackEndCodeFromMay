package com.superme.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategoryService {

    private static final String S3_BASE_URL = "https://your-bucket.s3.amazonaws.com/journal/";
    private final Map<String, List<Map<String, String>>> options = new HashMap<>();

    public CategoryService() {
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

    public Map<String, List<Map<String, String>>> getCategoryOptions() {
        return options;
    }

    public boolean addCategoryIcon(String category, String value, String iconPath) {
        options.putIfAbsent(category, new ArrayList<>());
        List<Map<String, String>> icons = options.get(category);
        boolean exists = icons.stream().anyMatch(icon -> icon.get("value").equals(value));
        if (!exists) {
            icons.add(icon(value, iconPath));
            return true;
        }
        return false;
    }

    public boolean deleteCategoryIcon(String category, String value) {
        List<Map<String, String>> icons = options.get(category);
        if (icons != null) {
            return icons.removeIf(icon -> icon.get("value").equals(value));
        }
        return false;
    }

    public boolean deleteCategory(String category) {
        return options.remove(category) != null;
    }

    public boolean updateCategoryIcon(String category, String oldValue, String newValue, String newIconPath) {
        List<Map<String, String>> icons = options.get(category);
        if (icons != null) {
            for (Map<String, String> icon : icons) {
                if (icon.get("value").equals(oldValue)) {
                    icon.put("value", newValue);
                    icon.put("icon", S3_BASE_URL + newIconPath);
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, String> icon(String value, String key) {
        return Map.of(
                "value", value,
                "icon", S3_BASE_URL + key
        );
    }
}