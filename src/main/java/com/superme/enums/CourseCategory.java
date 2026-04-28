package com.superme.enums;

public enum CourseCategory implements DisplayableEnum {

    MANAGING_MONEY("Managing Money"),
    COMMUNICATION("Communication"),
    MEDITATION("Meditation"),
    CULTURAL_VALUES("Cultural Values");

    private final String displayName;

    CourseCategory(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
