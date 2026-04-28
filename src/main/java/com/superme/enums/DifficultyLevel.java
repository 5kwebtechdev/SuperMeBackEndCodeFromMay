package com.superme.enums;

public enum DifficultyLevel {
    EASY("Easy", 2),  // 2 trophies for easy
    MEDIUM("Medium", 3), // 3 trophies for medium
    HARD("Hard", 4); // 4 trophies for hard

    private final String displayName;
    private final int trophyCount;

    DifficultyLevel(String displayName, int trophyCount) {
        this.displayName = displayName;
        this.trophyCount = trophyCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTrophyCount() {
        return trophyCount;
    }
}