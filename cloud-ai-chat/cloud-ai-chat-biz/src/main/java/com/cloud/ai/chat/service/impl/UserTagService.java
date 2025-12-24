package com.cloud.ai.chat.service.impl;


import com.cloud.ai.chat.domain.UserProfile;
import com.cloud.ai.chat.domain.UserTags;
import com.cloud.ai.chat.repository.UserProfileRepository;
import com.cloud.ai.chat.repository.UserTagsRepository;
import com.cloud.ai.chat.service.OccupationService;
import com.cloud.ai.chat.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签服务 - 负责用户标签的生成、更新、权重计算
 * @date 2025/01/16 10:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserTagService {

    private final UserTagsRepository userTagsRepository;
    private final UserProfileRepository profileRepository;
    private final OccupationService occupationService;


    /**
     * 计算并更新总权重
     * <p>
     * 程序员用户聊"编程" → 权重 4
     * 非程序员用户聊"编程" → 权重 1
     * 程序员用户聊"美食" → 权重 1
     */
    public void calculateTotalWeight(UserTags tag) {
        tag.setTotalWeight(tag.getBaseWeight().add(tag.getChatWeight()).add(tag.getFusionWeight()));
    }

    /**
     * 增加聊天权重（重叠时加倍）
     */
    public void incrementChatWeight(UserTags tag, boolean isOverlap) {
        BigDecimal increment = isOverlap ? BigDecimal.valueOf(2) : BigDecimal.ONE;
        tag.setChatWeight(tag.getChatWeight().add(increment));
        calculateTotalWeight(tag);
    }

    /**
     * 设置基础权重
     */
    public void setBaseWeight(UserTags tag, BigDecimal baseWeight) {
        ValidationUtils.validateWeight(baseWeight);
        tag.setBaseWeight(baseWeight);
        calculateTotalWeight(tag);
    }

    /**
     * 设置融合权重
     */
    public void setFusionWeight(UserTags tag, BigDecimal fusionWeight) {
        ValidationUtils.validateWeight(fusionWeight);
        tag.setFusionWeight(fusionWeight);
        calculateTotalWeight(tag);
    }

    /**
     * 基于用户画像生成基础标签
     */
    @Transactional
    public void generateProfileTags(Long userId, UserProfile profile) {
        log.info("基于用户画像生成基础标签，userId: {}", userId);

        List<String> profileTags = new ArrayList<>();

        // 基于职业生成标签
        if (profile.getOccupation() != null) {
            profileTags.addAll(occupationService.getTagsByCode(profile.getOccupation()));
        }

        // 基于爱好生成标签
        if (!CollectionUtils.isEmpty(profile.getHobbies())) {
            profileTags.addAll(profile.getHobbies());
        }

        // 基于年龄生成标签（移除性别标签，避免标签污染）
        List<String> ageTags = getAgeTags(profile.getAge());
        //去除重复标签
        profileTags = profileTags.stream().distinct().collect(Collectors.toList());
        // 保存基础标签（职业和爱好）
        for (String tagName : profileTags) {
            String cleanTagName = ValidationUtils.cleanTagName(tagName);
            if (cleanTagName != null) {
                ValidationUtils.validateTagName(cleanTagName);
                createOrUpdateProfileTag(userId, cleanTagName, UserTags.BASE_WEIGHT);
            }
        }

        // 保存年龄标签（使用较低权重）
        for (String tagName : ageTags) {
            String cleanTagName = ValidationUtils.cleanTagName(tagName);
            if (cleanTagName != null) {
                ValidationUtils.validateTagName(cleanTagName);
                createOrUpdateProfileTag(userId, cleanTagName, UserTags.DEMOGRAPHIC_WEIGHT);
            }
        }
    }

    /**
     * 基于聊天内容生成或更新标签
     */
    @Transactional
    public void updateChatTags(Long userId, List<String> chatTags) {
        log.info("基于聊天内容更新标签，userId: {}, chatTags: {}", userId, chatTags);


        List<UserTags> userTags = userTagsRepository.findTop5ByUserIdOrderByTotalWeightDesc(userId);
        Map<String, UserTags> tagMap = userTags.stream().collect(Collectors.toMap(UserTags::getTagName, Function.identity()));

        List<UserTags> tagsToSave = new ArrayList<>();

        for (String tagName : chatTags) {
            String cleanTagName = ValidationUtils.cleanTagName(tagName);
            if (cleanTagName == null) {
                continue;
            }
            ValidationUtils.validateTagName(cleanTagName);

            UserTags existingTag = tagMap.get(cleanTagName);

            if (existingTag != null) {
                // 更新现有标签
                boolean isOverlap = UserTags.profileSource.equals(existingTag.getSourceType()) || UserTags.fusionSource.equals(existingTag.getSourceType());

                if (isOverlap) {
                    // 重叠标签：增加融合权重
                    setFusionWeight(existingTag, existingTag.getFusionWeight().add(UserTags.OVERLAP_WEIGHT));
                    existingTag.setSourceType(UserTags.fusionSource);
                } else {
                    // 纯聊天标签：增加聊天权重
                    incrementChatWeight(existingTag, false);
                }

                tagsToSave.add(existingTag);
            } else {
                // 创建新的聊天标签
                UserTags newTag = new UserTags();
                newTag.setUserId(userId);
                newTag.setTagName(cleanTagName);
                newTag.setSourceType(UserTags.chatSource);
                newTag.setChatWeight(UserTags.CHAT_WEIGHT);
                calculateTotalWeight(newTag);

                tagsToSave.add(newTag);
            }
        }

        // 批量保存
        userTagsRepository.saveAll(tagsToSave);
    }

    /**
     * 创建或更新画像标签
     */
    private void createOrUpdateProfileTag(Long userId, String tagName, BigDecimal weight) {
        ValidationUtils.validateTagName(tagName);
        Optional<UserTags> existingTag = userTagsRepository.findByUserIdAndTagName(userId, tagName);

        if (existingTag.isPresent()) {
            // 更新现有标签的基础权重
            UserTags tag = existingTag.get();
            setBaseWeight(tag, weight);
            userTagsRepository.save(tag);
        } else {
            // 创建新标签
            UserTags tag = new UserTags();
            tag.setUserId(userId);
            tag.setTagName(tagName);
            tag.setSourceType(UserTags.profileSource);
            setBaseWeight(tag, weight);
            userTagsRepository.save(tag);
        }
    }


    /**
     * 获取用户的所有标签（前5个）
     */
    public List<UserTags> getUserTags(Long userId) {
        return userTagsRepository.findTop5ByUserIdOrderByTotalWeightDesc(userId);
    }

    /**
     * 获取用户的热门标签（权重排序）
     */
    public List<UserTags> getHotTags(Long userId, int limit) {
        ValidationUtils.validateRecommendationLimit(limit);
        return userTagsRepository.findTop5ByUserIdOrderByTotalWeightDesc(userId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * 获取用户基础画像标签名称列表（供其他服务调用）
     */
    public List<String> getProfileTagNames(Long userId) {
        Optional<UserProfile> profileOpt = profileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return new ArrayList<>();
        }

        UserProfile profile = profileOpt.get();
        List<String> tagNames = new ArrayList<>();

        // 基于职业生成标签
        if (profile.getOccupation() != null) {
            tagNames.addAll(occupationService.getTagsByCode(profile.getOccupation()));
        }

        // 基于爱好生成标签
        if (!CollectionUtils.isEmpty(profile.getHobbies())) {
            tagNames.addAll(profile.getHobbies());
        }

        // 基于年龄生成标签（移除性别标签，避免标签污染）
        tagNames.addAll(getAgeTags(profile.getAge()));

        return tagNames;
    }

    /**
     * 每月权重衰减任务
     * 每月1号凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void monthlyWeightDecay() {
        log.info("开始执行每月权重衰减任务");

        List<UserTags> allTags = userTagsRepository.findAll();
        int updatedCount = 0;

        for (UserTags tag : allTags) {
            // 衰减所有权重
            tag.setBaseWeight(tag.getBaseWeight().multiply(UserTags.DECAY_FACTOR).setScale(2, RoundingMode.HALF_UP));
            tag.setChatWeight(tag.getChatWeight().multiply(UserTags.DECAY_FACTOR).setScale(2, RoundingMode.HALF_UP));
            tag.setFusionWeight(tag.getFusionWeight().multiply(UserTags.DECAY_FACTOR).setScale(2, RoundingMode.HALF_UP));

            calculateTotalWeight(tag);
            userTagsRepository.save(tag);
            updatedCount++;
        }

        log.info("权重衰减任务完成，共更新 {} 个标签", updatedCount);
    }

    /**
     * 年龄标签生成（移除性别标签，避免标签污染）
     */
    private List<String> getAgeTags(Integer age) {
        List<String> tags = new ArrayList<>();

        if (age != null) {
            if (age < 25) {
                tags.add("年轻人");
            } else if (age < 35) {
                tags.add("职场新人");
            } else if (age < 50) {
                tags.add("职场精英");
            } else {
                tags.add("资深人士");
            }
        }

        return tags;
    }
}
