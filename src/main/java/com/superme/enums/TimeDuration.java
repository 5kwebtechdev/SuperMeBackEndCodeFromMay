package com.superme.enums;

public enum TimeDuration implements DisplayableEnum {
    SHORT("5 minutes", 5),
    MEDIUM("10 minutes", 10),
    LONG("12 minutes", 12);

    private final String displayName;
    private final int minutes;

    TimeDuration(String displayName, int minutes) {
        this.displayName = displayName;
        this.minutes = minutes;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public int getMinutes() {
        return minutes;
    }
}
