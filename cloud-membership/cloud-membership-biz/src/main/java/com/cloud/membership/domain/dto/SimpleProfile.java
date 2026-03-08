package com.cloud.membership.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户简单画像 - 用于AI推荐生成
 * @date 2026/2/21 15:49
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 性别
     */
    private String gender;

    /**
     * 年龄段
     */
    private String ageRange;

    /**
     * 居住地
     */
    private String location;

    /**
     * 职业名称
     */
    private String occupationName;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("性别：").append(this.getGender()).append("，");
        sb.append("年龄段：").append(this.getAgeRange()).append("，");
        sb.append("居住地：").append(this.getLocation()).append("，");
        sb.append("职业：").append(this.getOccupationName());
        return sb.toString();
    }
}
