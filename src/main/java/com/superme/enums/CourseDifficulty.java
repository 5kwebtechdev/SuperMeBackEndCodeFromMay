package com.superme.enums;

public enum CourseDifficulty implements DisplayableEnum {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    PRO("Pro");  // Fixed: removed semicolon, added comma

    private final String displayName;

    CourseDifficulty(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}