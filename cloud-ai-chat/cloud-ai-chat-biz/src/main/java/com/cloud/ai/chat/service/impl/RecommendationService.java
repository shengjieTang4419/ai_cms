package com.cloud.ai.chat.service.impl;


import com.cloud.ai.chat.domain.UserTags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐服务 - 基于用户标签生成AI推荐提问
 * @date 2025/01/16 11:30
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserProfileService userProfileService;
    private final AIChatService aiChatService;
    private final RedissonClient redissonClient;

    // Redis缓存相关常量
    private static final String RECOMMENDATION_CACHE_KEY_PREFIX = "recommendation:user:";
    private static final int CACHE_EXPIRE_HOURS = 24; // 缓存24小时

    /**
     * 生成个性化的推荐提问（带Redis缓存）
     *
     * @param userId 用户ID
     * @param limit  标签数量限制
     * @return 推荐结果，包含标签和生成的提问
     */
    public Map<String, Object> generatePersonalizedRecommendations(Long userId, int limit) {
        log.info("开始生成个性化推荐，userId: {}, limit: {}", userId, limit);

        // 1. 获取用户热门标签
        List<UserTags> hotTags = userProfileService.getHotTags(userId, limit);

        if (hotTags.isEmpty()) {
            log.warn("用户{}没有热门标签，返回空推荐", userId);
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("userId", userId);
            emptyResult.put("total", hotTags.size());
            emptyResult.put("generatedQuestions", List.of());
            return emptyResult;
        }

        // 2. 计算用户标签权重（用于缓存key）
        String userWeightKey = calculateUserWeightKey(hotTags);
        String cacheKey = RECOMMENDATION_CACHE_KEY_PREFIX + userId + ":" + userWeightKey;

        // 3. 尝试从Redis缓存获取
        try {
            Map<String, Object> cachedResult = (Map<String, Object>) redissonClient.getBucket(cacheKey).get();
            if (cachedResult != null) {
                log.info("从缓存获取推荐结果，userId: {}, cacheKey: {}", userId, cacheKey);
                return cachedResult;
            }
        } catch (Exception e) {
            log.warn("从Redis获取缓存失败，继续生成新推荐，userId: {}, error: {}", userId, e.getMessage());
        }

        // 4. 缓存未命中，生成新的推荐
        List<String> generatedQuestions = generateQuestionsFromTags(hotTags.stream()
                .map(UserTags::getTagName)
                .toList());

        // 5. 构建响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("total", hotTags.size());
        result.put("generatedQuestions", generatedQuestions);

        // 6. 将结果存入Redis缓存
        try {
            RBucket<Object> bucket = redissonClient.getBucket(cacheKey);
            bucket.set(result);
            Instant expireTime = Instant.now().plus(Duration.ofDays(1));
            bucket.expire(expireTime);
            log.info("推荐结果已缓存，userId: {}, cacheKey: {}, expire: {}小时",
                    userId, cacheKey, CACHE_EXPIRE_HOURS);
        } catch (Exception e) {
            log.warn("缓存推荐结果失败，userId: {}, error: {}", userId, e.getMessage());
        }

        log.info("个性化推荐生成完成，userId: {}, 标签数: {}, 提问数: {}",
                userId, hotTags.size(), generatedQuestions.size());

        return result;
    }

    /**
     * 计算用户标签权重key（用于缓存标识）
     * 当用户标签权重变化不大时，可以复用缓存
     * 以10作为区间
     *
     * @param hotTags 用户热门标签
     * @return 权重key
     */
    private String calculateUserWeightKey(List<UserTags> hotTags) {
        // 计算标签总权重（使用totalWeight字段）
        int totalWeight = hotTags.stream()
                .mapToInt(tag -> tag.getTotalWeight() != null ? tag.getTotalWeight().intValue() : 1)
                .sum();

        // 生成权重key
        return String.valueOf(totalWeight / 10 * 10);
    }

    /**
     * 根据用户标签生成个性推荐提问
     *
     * @param tagNames 用户标签列表
     * @return 生成的推荐提问列表
     */
    private List<String> generateQuestionsFromTags(List<String> tagNames) {
        if (tagNames.isEmpty()) {
            return List.of();
        }

        // 构建AI提示词
        String tagsStr = String.join("、", tagNames);

        String prompt = String.format("""
                根据以下用户兴趣标签，为每个标签生成一个吸引人的、个性化的推荐问题。
                要求：
                1. 每个问题都要很有趣、有吸引力，能激发用户的探索欲
                2. 问题要具体、有价值，能提供实际帮助
                3. 语言要生动活泼，带有情感色彩
                4. 不要使用过于正式的语气，要像朋友一样推荐
                
                用户兴趣标签：%s
                
                请为每个标签生成一个推荐问题，每个问题独占一行。
                例如：
                旅游 -> 3天云南日照金山全路程攻略Get！
                编程 -> 论MESI嗅探机制的作用和原理
                服装 -> 2025巴黎服装新标向
                
                生成的推荐问题：
                """, tagsStr);

        try {
            // 调用AI服务生成推荐提问
            String aiResponse = aiChatService.simpleChat(prompt);

            // 解析AI响应，提取生成的提问
            return parseGeneratedQuestions(aiResponse);

        } catch (Exception e) {
            log.error("调用AI生成推荐提问失败，tags: {}", tagNames, e);

            // 如果AI调用失败，返回基于模板的推荐提问
            return generateFallbackQuestions(tagNames);
        }
    }

    /**
     * 解析AI生成的推荐提问
     *
     * @param aiResponse AI响应内容
     * @return 解析后的推荐提问列表
     */
    private List<String> parseGeneratedQuestions(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return List.of();
        }

        // 按行分割，并过滤掉空行和无关内容
        return aiResponse.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("生成") && !line.startsWith("请为"))
                .toList();
    }

    /**
     * 生成备用的推荐提问（当AI调用失败时使用）
     *
     * @param tagNames 用户标签列表
     * @return 备用的推荐提问列表
     */
    private List<String> generateFallbackQuestions(List<String> tagNames) {
        return tagNames.stream()
                .map(tag -> String.format("关于%s，你知道有哪些有趣的玩法？", tag))
                .toList();
    }
}
