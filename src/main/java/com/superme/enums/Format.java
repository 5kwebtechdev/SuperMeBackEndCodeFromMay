package com.superme.enums;

public enum Format implements DisplayableEnum {
    TEXT_BASED("Text Based"),
    VIDEO("Video"),
    AUDIO("Audio"),
    PDF("PDF"),
    QUIZ("Quiz");

    private final String displayName;

    Format(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
