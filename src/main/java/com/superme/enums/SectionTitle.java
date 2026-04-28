package com.superme.enums;

public enum SectionTitle implements DisplayableEnum {
    TODAYS_CHALLENGE("Today's Challenge"),
    TRENDING("Trending"),
    RECENTLY_ADDED("Recently Added");

    private final String displayName;

    SectionTitle(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
