package com.cloud.membership.service.impl;


import com.cloud.ai.chat.api.RemoteStereotypeTagFeignService;
import com.cloud.ai.chat.api.domain.StereotypeTagRequest;
import com.cloud.ai.chat.api.domain.StereotypeTagResponse;
import com.cloud.membership.domain.*;
import com.cloud.membership.domain.enums.AgeRange;
import com.cloud.membership.domain.enums.Gender;
import com.cloud.membership.repository.OccupationRepository;
import com.cloud.membership.repository.StereotypeTemplateRepository;
import com.cloud.membership.service.ITemplateUserProfile;
import com.cloud.memebership.domain.TagPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象模板服务实现
 * 核心功能：
 * 1. 初始化系统内置模板（遍历性别、年龄段、常用职业组合）
 * 2. 根据用户特征匹配模板
 * 3. 将模板标签转换为用户初始标签权重
 * @date 2026/2/5 15:38
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateUserProfileServiceImpl implements ITemplateUserProfile {

    private final StereotypeTemplateRepository templateRepository;
    private final OccupationRepository occupationRepository;
    private final RemoteStereotypeTagFeignService remoteStereotypeTagFeignService;

    /**
     * 初始化系统内置模板
     * 遍历所有性别、年龄段、常用职业组合
     */
    @Override
    public void doInit() {
        log.info("开始初始化刻板印象模板...");

        // 获取所有常用职业
        List<OccupationEntity> occupations = occupationRepository.findByStatusOrderBySortOrder(1);
        if (CollectionUtils.isEmpty(occupations)) {
            log.warn("未找到启用的职业，跳过模板初始化");
            return;
        }

        int createdCount = 0;
        int skippedCount = 0;

        // 遍历所有组合：性别 × 年龄段 × 职业
        for (Gender gender : Gender.values()) {
            for (AgeRange ageRange : AgeRange.values()) {
                for (OccupationEntity occupation : occupations) {
                    String templateKey = StereotypeTemplate.generateTemplateKey(gender, ageRange, occupation.getCode());

                    // 检查是否已存在
                    if (templateRepository.existsByTemplateKey(templateKey)) {
                        skippedCount++;
                        continue;
                    }

                    // 生成模板
                    StereotypeTemplate template = buildTemplate(gender, ageRange, occupation);
                    templateRepository.save(template);
                    createdCount++;
                }
            }
        }

        log.info("刻板印象模板初始化完成，新建: {}, 跳过: {}", createdCount, skippedCount);
    }

    /**
     * 根据用户画像快速匹配模板，获取初始标签权重
     * 优先匹配已存在的模板，如果没有匹配到则实时调用AI生成
     */
    @Override
    public List<UserTagsDimension> fastCreate(TemplateProfile templateProfile) {
        if (templateProfile == null) {
            return Collections.emptyList();
        }

        // 1. 解析用户特征
        Gender gender = Gender.fromLabel(templateProfile.getGender());
        AgeRange ageRange = determineAgeRange(templateProfile.getMinAge(), templateProfile.getMaxAge());
        Integer occupationCode = templateProfile.getOccupation();

        // 2. 尝试匹配已存在的模板
        StereotypeTemplate template = matchTemplate(gender, ageRange, occupationCode);
        if (template != null) {
            log.info("匹配到刻板印象模板: {}", template.getTemplateKey());
            return convertToUserTagsDimensions(template);
        }

        // 3. 没有匹配到模板，实时调用AI生成标签
        log.info("未找到匹配的模板，实时调用AI生成标签: gender={}, ageRange={}, occupation={}, location={}",
                gender, ageRange, occupationCode, templateProfile.getLocation());

        // 获取职业名称
        String occupationName = templateProfile.getOccupationName();
        if (occupationName == null && occupationCode != null) {
            OccupationEntity occupation = occupationRepository.findByCode(occupationCode).orElse(null);
            occupationName = occupation != null ? occupation.getName() : null;
        }

        // 构建AI请求
        StereotypeTagRequest request = StereotypeTagRequest.builder()
                .gender(gender != null ? gender.getLabel() : null)
                .ageRangeLabel(ageRange != null ? ageRange.getLabel() : null)
                .minAge(templateProfile.getMinAge())
                .maxAge(templateProfile.getMaxAge())
                .occupationName(occupationName)
                .occupationCode(occupationCode)
                .location(templateProfile.getLocation())
                .hobbies(templateProfile.getHobbies())
                .build();

        try {
            StereotypeTagResponse aiResponse = remoteStereotypeTagFeignService.generateStereotypeTags(request);
            if (aiResponse != null && aiResponse.isSuccess() && !CollectionUtils.isEmpty(aiResponse.getTags())) {
                return convertAIResponseToUserTagsDimensions(aiResponse);
            }
        } catch (Exception e) {
            log.error("实时调用AI生成标签失败", e);
        }

        // 4. AI也失败了，返回空
        log.warn("AI生成标签失败，无法为用户生成初始标签");
        return Collections.emptyList();
    }

    /**
     * 将AI响应转换为用户标签维度
     */
    private List<UserTagsDimension> convertAIResponseToUserTagsDimensions(StereotypeTagResponse aiResponse) {
        if (aiResponse == null || CollectionUtils.isEmpty(aiResponse.getTags())) {
            return Collections.emptyList();
        }

        Map<String, Double> weights = aiResponse.getTagWeights();
        LocalDateTime now = LocalDateTime.now();

        return aiResponse.getTags().stream()
                .map(tagPathStr -> {
                    // 构建TagPath
                    TagPath tagPath = new TagPath();
                    tagPath.setKey(tagPathStr);
                    tagPath.setPath(Arrays.asList(tagPathStr.split("/")));

                    // 设置权重
                    Double weight = weights != null ? weights.getOrDefault(tagPathStr, 0.5) : 0.5;

                    return UserTagsDimension.builder()
                            .pathKey(tagPathStr)
                            .tagPath(tagPath)
                            .totalWeight(weight)
                            .lastActiveTime(now)
                            .createdTime(now)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据用户画像匹配最佳模板
     */
    @Override
    public StereotypeTemplate matchTemplate(UserProfile userProfile) {
        if (userProfile == null) {
            return null;
        }

        Gender gender = Gender.fromLabel(userProfile.getGender());
        AgeRange ageRange = AgeRange.fromAge(userProfile.getAge());
        Integer occupationCode = userProfile.getOccupation();

        return matchTemplate(gender, ageRange, occupationCode);
    }

    /**
     * 根据特征匹配模板
     * 三级降级策略 匹配模版
     */
    @Override
    public StereotypeTemplate matchTemplate(Gender gender, AgeRange ageRange, Integer occupationCode) {
        // 1. 精确匹配
        Optional<StereotypeTemplate> exactMatch = templateRepository
                .findByGenderAndAgeRangeAndOccupationCodeAndStatus(gender, ageRange, occupationCode, 1);
        if (exactMatch.isPresent()) {
            log.debug("精确匹配到模板: {}", exactMatch.get().getTemplateKey());
            return exactMatch.get();
        }

        // 2. 模糊匹配（按优先级降序）
        List<StereotypeTemplate> fuzzyMatches = templateRepository
                .findMatchingTemplates(gender, ageRange, occupationCode);
        if (!CollectionUtils.isEmpty(fuzzyMatches)) {
            StereotypeTemplate best = selectBestMatch(fuzzyMatches, gender, ageRange, occupationCode);
            log.debug("模糊匹配到模板: {}", best.getTemplateKey());
            return best;
        }

        // 3. 降级匹配：只匹配职业
        if (occupationCode != null) {
            List<StereotypeTemplate> occupationMatches = templateRepository
                    .findByOccupationCodeAndStatus(occupationCode, 1);
            if (!CollectionUtils.isEmpty(occupationMatches)) {
                log.debug("降级匹配（仅职业）到模板: {}", occupationMatches.get(0).getTemplateKey());
                return occupationMatches.get(0);
            }
        }

        log.debug("未找到匹配的模板");
        return null;
    }

    /**
     * 获取所有启用的模板
     */
    @Override
    public List<StereotypeTemplate> getAllActiveTemplates() {
        return templateRepository.findByStatusOrderByPriorityDesc(1);
    }

    /**
     * 生成单个模板（供AI调用）
     */
    @Override
    @Transactional
    public StereotypeTemplate generateTemplate(Gender gender, AgeRange ageRange, Integer occupationCode) {
        OccupationEntity occupation = null;
        if (occupationCode != null) {
            occupation = occupationRepository.findByCode(occupationCode).orElse(null);
        }

        StereotypeTemplate template = buildTemplate(gender, ageRange, occupation);
        return templateRepository.save(template);
    }

    /**
     * 批量生成模板
     */
    @Override
    public List<StereotypeTemplate> batchGenerateTemplates() {
        doInit();
        return getAllActiveTemplates();
    }

    /**
     * 构建刻板印象模板（通过AI生成标签）
     */
    private StereotypeTemplate buildTemplate(Gender gender, AgeRange ageRange, OccupationEntity occupation) {
        StereotypeTemplate template = new StereotypeTemplate();

        Integer occupationCode = occupation != null ? occupation.getCode() : null;
        String templateKey = StereotypeTemplate.generateTemplateKey(gender, ageRange, occupationCode);
        template.setTemplateKey(templateKey);

        // 生成模板名称
        String templateName = buildTemplateName(gender, ageRange, occupation);
        template.setTemplateName(templateName);

        template.setGender(gender);
        template.setAgeRange(ageRange);
        template.setOccupationCode(occupationCode);

        // 通过AI生成预设标签和权重
        StereotypeTagResponse aiResponse = generateTagsWithAI(gender, ageRange, occupation, null, null);

        if (aiResponse != null && aiResponse.isSuccess() && !CollectionUtils.isEmpty(aiResponse.getTags())) {
            template.setPresetTags(aiResponse.getTags());
            template.setTagWeights(aiResponse.getTagWeights());
            template.setDescription(aiResponse.getProfileDescription());
            template.setSource("AI");
        }
        //降级方案毫无意义 命中率太低
        template.setStatus(1);
        template.setPriority(calculatePriority(gender, ageRange, occupationCode));

        return template;
    }

    /**
     * 通过AI生成刻板印象标签
     */
    private StereotypeTagResponse generateTagsWithAI(Gender gender, AgeRange ageRange,
                                                     OccupationEntity occupation,
                                                     String location, List<String> hobbies) {
        try {
            //刻板印象标签时 职业和爱好都是空的
            StereotypeTagRequest request = StereotypeTagRequest.builder()
                    .gender(gender != null ? gender.getLabel() : null)
                    .ageRangeLabel(ageRange != null ? ageRange.getLabel() : null)
                    .minAge(ageRange != null ? ageRange.getMinAge() : null)
                    .maxAge(ageRange != null ? ageRange.getMaxAge() : null)
                    .occupationName(occupation != null ? occupation.getName() : null)
                    .occupationCode(occupation != null ? occupation.getCode() : null)
                    .location(location)
                    .hobbies(hobbies)
                    .build();

            return remoteStereotypeTagFeignService.generateStereotypeTags(request);
        } catch (Exception e) {
            log.error("调用AI生成标签服务失败", e);
            return null;
        }
    }

    /**
     * 生成模板名称
     */
    private String buildTemplateName(Gender gender, AgeRange ageRange, OccupationEntity occupation) {
        StringBuilder name = new StringBuilder();
        if (ageRange != null) {
            name.append(ageRange.getLabel());
        }
        if (gender != null) {
            name.append(gender.getLabel()).append("性");
        }
        if (occupation != null) {
            name.append(occupation.getName());
        }
        return name.toString();
    }

    /**
     * 计算模板优先级
     * 特征越完整，优先级越高
     */
    private int calculatePriority(Gender gender, AgeRange ageRange, Integer occupationCode) {
        int priority = 0;
        if (gender != null) priority += 10;
        if (ageRange != null) priority += 20;
        if (occupationCode != null) priority += 30;
        return priority;
    }

    /**
     * 确定年龄段
     */
    private AgeRange determineAgeRange(Integer minAge, Integer maxAge) {
        if (minAge == null && maxAge == null) {
            return null;
        }
        // 取中间值
        int midAge = (minAge != null ? minAge : 0) + (maxAge != null ? maxAge : 100);
        midAge = midAge / 2;
        return AgeRange.fromAge(midAge);
    }

    /**
     * 选择最佳匹配模板
     */
    private StereotypeTemplate selectBestMatch(List<StereotypeTemplate> candidates,
                                               Gender gender, AgeRange ageRange, Integer occupationCode) {
        return candidates.stream()
                .max(Comparator.comparingInt(t -> calculateMatchScore(t, gender, ageRange, occupationCode)))
                .orElse(candidates.get(0));
    }

    /**
     * 计算匹配分数
     */
    private int calculateMatchScore(StereotypeTemplate template, Gender gender,
                                    AgeRange ageRange, Integer occupationCode) {
        int score = 0;
        if (Objects.equals(template.getGender(), gender)) score += 10;
        if (Objects.equals(template.getAgeRange(), ageRange)) score += 20;
        if (Objects.equals(template.getOccupationCode(), occupationCode)) score += 30;
        return score;
    }

    /**
     * 将模板转换为用户标签维度
     */
    private List<UserTagsDimension> convertToUserTagsDimensions(StereotypeTemplate template) {
        if (template == null || CollectionUtils.isEmpty(template.getPresetTags())) {
            return Collections.emptyList();
        }

        Map<String, Double> weights = template.getTagWeights();
        LocalDateTime now = LocalDateTime.now();

        return template.getPresetTags().stream()
                .map(tagPathStr -> {
                    // 构建TagPath
                    TagPath tagPath = new TagPath();
                    tagPath.setKey(tagPathStr);
                    tagPath.setPath(Arrays.asList(tagPathStr.split("/")));

                    // 设置权重
                    Double weight = weights != null ? weights.getOrDefault(tagPathStr, 0.5) : 0.5;

                    return UserTagsDimension.builder()
                            .pathKey(tagPathStr)
                            .tagPath(tagPath)
                            .totalWeight(weight)
                            .lastActiveTime(now)
                            .createdTime(now)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
