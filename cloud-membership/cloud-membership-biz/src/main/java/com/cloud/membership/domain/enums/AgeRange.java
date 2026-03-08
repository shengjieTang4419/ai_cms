package com.cloud.membership.domain.enums;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 年龄段枚举
 * @date 2026/2/5 16:00
 */
public enum AgeRange {

    CHILD(0, 12, "儿童"),
    TEENAGER(13, 17, "青少年"),
    YOUNG_ADULT(18, 25, "青年"),
    ADULT(26, 35, "成年"),
    MIDDLE_AGED(36, 50, "中年"),
    SENIOR(51, 65, "中老年"),
    ELDERLY(66, 120, "老年");

    private final int minAge;
    private final int maxAge;
    private final String label;

    AgeRange(int minAge, int maxAge, String label) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.label = label;
    }

    public int getMinAge() {
        return minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public String getLabel() {
        return label;
    }

    public static AgeRange fromAge(Integer age) {
        if (age == null) {
            return null;
        }
        for (AgeRange range : values()) {
            if (age >= range.minAge && age <= range.maxAge) {
                return range;
            }
        }
        return ADULT;
    }

    public boolean contains(int age) {
        return age >= minAge && age <= maxAge;
    }
}
