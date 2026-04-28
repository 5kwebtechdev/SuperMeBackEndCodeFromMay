package com.superme.enums;

public enum AgeGroup implements DisplayableEnum {
    BELOW_11("Below 11"),
    AGE_11_TO_13("11-13"),
    AGE_14_TO_15("14-15"),
    AGE_16_TO_17("16-17"),
    AGE_18_PLUS("18+"),

    ALL("All Age Groups");


    private final String displayName;

    AgeGroup(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    // ✅ Add static method to get AgeGroup from age value
    public static AgeGroup fromAge(int age) {
        if (age < 11) {
            return BELOW_11;
        } else if (age <= 13) {
            return AGE_11_TO_13;
        } else if (age <= 15) {
            return AGE_14_TO_15;
        } else if (age <= 17) {
            return AGE_16_TO_17;
        } else {
            return AGE_18_PLUS;
        }
    }
}
