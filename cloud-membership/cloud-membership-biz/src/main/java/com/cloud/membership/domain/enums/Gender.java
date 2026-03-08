package com.cloud.membership.domain.enums;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 性别枚举
 * @date 2026/2/5 16:00
 */
public enum Gender {

    MALE("男"),
    FEMALE("女");
    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Gender fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (Gender gender : values()) {
            if (gender.label.equals(label)) {
                return gender;
            }
        }
        return MALE;
    }
}
