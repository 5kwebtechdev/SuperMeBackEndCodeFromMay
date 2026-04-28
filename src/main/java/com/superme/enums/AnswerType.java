package com.superme.enums;

public enum AnswerType implements DisplayableEnum {
    MCQ("Multiple Choice", 4),
    TRUE_FALSE("True or False", 2),
    VISUALS("Visual Choice", 4);

    private final String displayName;
    private final int optionCount;

    AnswerType(String displayName, int optionCount) {
        this.displayName = displayName;
        this.optionCount = optionCount;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public int getOptionCount() {
        return optionCount;
    }
}
