package com.superme.enums;

public enum Department implements DisplayableEnum{
    HR("Human Resources"),
    IT("Information Technology"),
    MARKETING("Marketing"),
    FINANCE("Finance"),
    OPERATIONS("Operations"),
    CUSTOMER_SUPPORT("Customer Support");


    private final String displayName;

    Department(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}

