package com.superme.enums;

public enum ContentType {
    QUIZ("Quiz"),
    PUZZLE("Puzzle");

    private final String displayName;

    ContentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}