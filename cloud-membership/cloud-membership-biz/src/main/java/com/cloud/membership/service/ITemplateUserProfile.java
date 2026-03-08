package com.cloud.membership.service;


import com.cloud.membership.domain.StereotypeTemplate;
import com.cloud.membership.domain.TemplateProfile;
import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.domain.UserTagsDimension;
import com.cloud.membership.domain.enums.AgeRange;
import com.cloud.membership.domain.enums.Gender;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象模板服务接口
 * @date 2026/2/5 15:38
 */
public interface ITemplateUserProfile {

    /**
     * 初始化系统内置模板
     * 遍历所有性别、年龄段、常用职业组合，生成刻板印象模板
     */
    void doInit();

    /**
     * 根据用户画像快速匹配模板，获取初始标签权重
     */
    List<UserTagsDimension> fastCreate(TemplateProfile templateProfile);

    /**
     * 根据用户画像匹配最佳模板
     */
    StereotypeTemplate matchTemplate(UserProfile userProfile);

    /**
     * 根据特征精确匹配模板
     */
    StereotypeTemplate matchTemplate(Gender gender, AgeRange ageRange, Integer occupationCode);

    /**
     * 获取所有启用的模板
     */
    List<StereotypeTemplate> getAllActiveTemplates();

    /**
     * 生成单个模板（供AI调用）
     */
    StereotypeTemplate generateTemplate(Gender gender, AgeRange ageRange, Integer occupationCode);

    /**
     * 批量生成模板（供AI调用）
     */
    List<StereotypeTemplate> batchGenerateTemplates();
}
