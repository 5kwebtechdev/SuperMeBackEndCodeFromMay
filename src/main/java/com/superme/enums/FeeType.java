package com.superme.enums;

public enum FeeType {
    PER_HOUR("Per Hour"),
    PER_MONTH("Per Month"),
    PER_SUBJECT("Per Subject");

    private final String displayName;

    FeeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

