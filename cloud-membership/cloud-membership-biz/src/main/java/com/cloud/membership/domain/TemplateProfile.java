package com.cloud.membership.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 模版用户特征（用于匹配刻板印象模板）
 * @date 2026/2/5 15:39
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateProfile {

    /**
     * 年龄段
     */
    private Integer minAge;
    private Integer maxAge;

    /**
     * 性别：男、女
     */
    private String gender;

    /**
     * 居住地
     */
    private String location;

    /**
     * 职业代码
     */
    private Integer occupation;

    /**
     * 职业名称
     */
    private String occupationName;

    /**
     * 爱好列表
     */
    private List<String> hobbies;

    /**
     * 从UserProfile构建TemplateProfile
     */
    public static TemplateProfile fromUserProfile(UserProfile userProfile) {
        if (userProfile == null) {
            return null;
        }
        return TemplateProfile.builder()
                .gender(userProfile.getGender())
                .minAge(userProfile.getAge())
                .maxAge(userProfile.getAge())
                .location(userProfile.getLocation())
                .occupation(userProfile.getOccupation())
                .hobbies(userProfile.getHobbies())
                .build();
    }
}
