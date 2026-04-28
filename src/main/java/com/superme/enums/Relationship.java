package com.superme.enums;

public enum Relationship implements DisplayableEnum {
    CHILD("Child"),
    PARENT("Parent"),
    SELF("Self");

    private final String displayName;

    Relationship(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}

