package com.superme.enums;

public enum Category implements DisplayableEnum {
    QUIZ("Quiz"),
    PUZZLE("Puzzle"),
    ARTICLE("Article");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
