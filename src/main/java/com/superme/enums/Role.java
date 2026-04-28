package com.superme.enums;

public enum Role implements DisplayableEnum {
    USER("User"),
    ADMIN("Admin"),
    SUPER_ADMIN("Super Admin"),
    MANAGER("Manager"),
    EXECUTIVE("Executive");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}

