package com.superme.enums;

public enum Status implements DisplayableEnum{
    DRAFT("Draft"),
    VERIFICATION_PENDING("Verification Pending"),
    APPROVED("Approved"),

    PUBLISHED("Published"),
    REJECTED("Rejected");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
