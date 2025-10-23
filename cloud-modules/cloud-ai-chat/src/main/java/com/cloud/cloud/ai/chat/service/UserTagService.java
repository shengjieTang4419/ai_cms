package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.domain.Occupation;
import com.cloud.cloud.ai.chat.domain.UserProfile;
import com.cloud.cloud.ai.chat.domain.UserTags;
import com.cloud.cloud.ai.chat.repository.UserTagsRepository;
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

    /**
     * 权重常量
     */
    private static final BigDecimal BASE_WEIGHT = BigDecimal.ONE;      // 基础权重（来自用户画像）
    private static final BigDecimal CHAT_WEIGHT = BigDecimal.ONE;      // 聊天权重
    private static final BigDecimal OVERLAP_WEIGHT = BigDecimal.valueOf(2);   // 重叠权重（基础画像与聊天标签重叠时，为了使得热点标签更突出）
    private static final BigDecimal DEMOGRAPHIC_WEIGHT = BigDecimal.valueOf(0.2); // 人口统计标签权重（年龄标签）
    private static final BigDecimal DECAY_FACTOR = BigDecimal.valueOf(0.5); // 衰减因子（每月衰减一半，快速使得过期失效热点数据失效）

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
        tag.setBaseWeight(baseWeight);
        calculateTotalWeight(tag);
    }

    /**
     * 设置融合权重
     */
    public void setFusionWeight(UserTags tag, BigDecimal fusionWeight) {
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
            profileTags.addAll(Occupation.getTagsByCode(profile.getOccupation()));
        }

        // 基于爱好生成标签
        if (!CollectionUtils.isEmpty(profile.getHobbies())) {
            profileTags.addAll(profile.getHobbies());
        }

        // 基于年龄生成标签（移除性别标签，避免标签污染）
        List<String> ageTags = getAgeTags(profile.getAge());

        // 保存基础标签（职业和爱好）
        for (String tagName : profileTags) {
            createOrUpdateProfileTag(userId, tagName);
        }

        // 保存年龄标签（使用较低权重）
        for (String tagName : ageTags) {
            createOrUpdateAgeTag(userId, tagName);
        }
    }

    /**
     * 基于聊天内容生成或更新标签
     */
    @Transactional
    public void updateChatTags(Long userId, List<String> chatTags) {
        log.info("基于聊天内容更新标签，userId: {}, chatTags: {}", userId, chatTags);


        List<UserTags> userTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId);
        Map<String, UserTags> tagMap = userTags.stream().collect(Collectors.toMap(UserTags::getTagName, Function.identity()));

        List<UserTags> tagsToSave = new ArrayList<>();

        for (String tagName : chatTags) {
            UserTags existingTag = tagMap.get(tagName);

            if (existingTag != null) {
                // 更新现有标签
                boolean isOverlap = "PROFILE".equals(existingTag.getSourceType()) || "FUSION".equals(existingTag.getSourceType());

                if (isOverlap) {
                    // 重叠标签：增加融合权重
                    setFusionWeight(existingTag, existingTag.getFusionWeight().add(OVERLAP_WEIGHT));
                    existingTag.setSourceType("FUSION");
                } else {
                    // 纯聊天标签：增加聊天权重
                    incrementChatWeight(existingTag, false);
                }

                tagsToSave.add(existingTag);
            } else {
                // 创建新的聊天标签
                UserTags newTag = new UserTags();
                newTag.setUserId(userId);
                newTag.setTagName(tagName);
                newTag.setSourceType("CHAT");
                newTag.setChatWeight(CHAT_WEIGHT);
                calculateTotalWeight(newTag);

                tagsToSave.add(newTag);
            }
        }

        // 批量保存
        userTagsRepository.saveAll(tagsToSave);
    }

    /**
     * 创建或更新画像标签（职业和爱好）
     * 我考虑到后续可能存在的修改用户个人信息（更新职业标签），不然他应该仅仅只是创建
     */
    private void createOrUpdateProfileTag(Long userId, String tagName) {
        Optional<UserTags> existingTag = userTagsRepository.findByUserIdAndTagName(userId, tagName);

        if (existingTag.isPresent()) {
            // 更新现有标签的基础权重
            UserTags tag = existingTag.get();
            setBaseWeight(tag, BASE_WEIGHT);
            userTagsRepository.save(tag);
        } else {
            // 创建新标签
            UserTags tag = new UserTags();
            tag.setUserId(userId);
            tag.setTagName(tagName);
            tag.setSourceType("PROFILE");
            setBaseWeight(tag, BASE_WEIGHT);
            userTagsRepository.save(tag);
        }
    }

    /**
     * 创建或更新年龄标签（使用较低权重）
     */
    private void createOrUpdateAgeTag(Long userId, String tagName) {
        Optional<UserTags> existingTag = userTagsRepository.findByUserIdAndTagName(userId, tagName);

        if (existingTag.isPresent()) {
            // 更新现有标签的基础权重
            UserTags tag = existingTag.get();
            tag.setBaseWeight(DEMOGRAPHIC_WEIGHT);
            calculateTotalWeight(tag);
            userTagsRepository.save(tag);
        } else {
            // 创建新标签
            UserTags tag = new UserTags();
            tag.setUserId(userId);
            tag.setTagName(tagName);
            tag.setSourceType("PROFILE");
            tag.setBaseWeight(DEMOGRAPHIC_WEIGHT);
            calculateTotalWeight(tag);
            userTagsRepository.save(tag);
        }
    }


    /**
     * 获取用户的所有标签
     */
    public List<UserTags> getUserTags(Long userId) {
        return userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId);
    }

    /**
     * 获取用户的热门标签（权重排序）
     */
    public List<UserTags> getHotTags(Long userId, int limit) {
        return userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId)
                .stream()
                .limit(limit)
                .toList();
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
            tag.setBaseWeight(tag.getBaseWeight().multiply(DECAY_FACTOR).setScale(2, RoundingMode.HALF_UP));
            tag.setChatWeight(tag.getChatWeight().multiply(DECAY_FACTOR).setScale(2, RoundingMode.HALF_UP));
            tag.setFusionWeight(tag.getFusionWeight().multiply(DECAY_FACTOR).setScale(2, RoundingMode.HALF_UP));

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
