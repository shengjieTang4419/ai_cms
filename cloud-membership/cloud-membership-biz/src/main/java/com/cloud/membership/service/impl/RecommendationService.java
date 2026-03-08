package com.cloud.membership.service.impl;


import com.cloud.ai.chat.api.RemoteRecommendationFeignService;
import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.domain.UserTags;
import com.cloud.membership.domain.dto.SimpleProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性推荐服务
 * @date 2026/2/21 09:57
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

    private final UserTagService userTagService;
    private final UserProfileService userProfileService;
    private final RedissonClient redissonClient;
    private final RemoteRecommendationFeignService remoteRecommendationFeignService;

    private static final String RECOMMENDATION_CACHE_PREFIX = "recommendation:";
    private static final Duration CACHE_EXPIRE_DURATION = Duration.ofDays(1);

    /**
     * 生成个性推荐
     * 为什么带有缓存，因为用户shot首页的概率是高的,触发标签变化是少的
     * 懒加载模式，cache Aside模式
     *
     * @param userId 用户ID
     * @param limit  标签数量限制
     * @return 推荐结果，包含标签和生成的提问
     */
    public List<String> lazyGeneratePersonalRecommendations(Long userId, int limit) {
        log.info("开始生成个性化推荐，userId: {}, limit: {}", userId, limit);

        // 1. 获取用户热门标签和画像
        List<UserTags> hotTags = userTagService.getHotTags(userId, limit);
        if (hotTags.isEmpty()) {
            log.warn("用户{}没有热门标签，返回空推荐", userId);
            return List.of();
        }

        UserProfile userProfile = userProfileService.getCurrentUserProfile(userId);
        SimpleProfile simpleProfile = userProfile.getSimpleProfile();
        String simpleProfileStr = simpleProfile.toString();

        // 2. Cache Aside 模式：先查缓存，缓存未命中则异步调用AI生成
        List<String> recommendations = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (UserTags userTag : hotTags) {
            String cacheKey = buildCacheKey(userId, simpleProfileStr, userTag.getPathKey());
            RBucket<String> bucket = redissonClient.getBucket(cacheKey);
            String cachedQuestion = bucket.get();

            if (cachedQuestion != null) {
                log.debug("缓存命中，tag: {}, question: {}", userTag.getPathKey(), cachedQuestion);
                recommendations.add(cachedQuestion);
            } else {
                log.debug("缓存未命中，异步生成推荐问题，tag: {}", userTag.getPathKey());
                CompletableFuture<Void> future = generateAndCacheQuestionAsync(
                        simpleProfileStr, userTag.getPathKey(), cacheKey);
                futures.add(future);
            }
        }

        // 3. 等待所有异步任务完成（可选：设置超时）
        if (!futures.isEmpty()) {
            log.info("等待{}个异步AI生成任务完成", futures.size());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 4. 重新从缓存获取新生成的推荐
            for (UserTags userTag : hotTags) {
                String cacheKey = buildCacheKey(userId, simpleProfileStr, userTag.getPathKey());
                RBucket<String> bucket = redissonClient.getBucket(cacheKey);
                String question = bucket.get();
                if (question != null && !recommendations.contains(question)) {
                    recommendations.add(question);
                }
            }
        }

        log.info("个性化推荐生成完成，userId: {}, 推荐数: {}", userId, recommendations.size());
        return recommendations;
    }

    /**
     * 异步生成推荐问题并缓存
     *
     * @param simpleProfile 用户简单画像
     * @param pathKey       标签路径
     * @param cacheKey      缓存key
     * @return CompletableFuture
     */
    @Async
    public CompletableFuture<Void> generateAndCacheQuestionAsync(String simpleProfile,
                                                                 String pathKey,
                                                                 String cacheKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("异步调用AI生成推荐问题，pathKey: {}", pathKey);
                String question = remoteRecommendationFeignService.generateQuestionsFromTags(
                        simpleProfile, pathKey);

                if (question != null && !question.isEmpty()) {
                    // 提取pathKey最后一个斜杠后的内容
                    String[] pathParts = pathKey.split("/");
                    String lastPathSegment = pathParts[pathParts.length - 1];

                    // 拼接最后一段路径和问题
                    String questionWithPath = lastPathSegment + ":" + question;

                    RBucket<String> bucket = redissonClient.getBucket(cacheKey);
                    bucket.set(questionWithPath, CACHE_EXPIRE_DURATION);
                    log.info("推荐问题已缓存，pathKey: {}, lastSegment: {}, question: {}",
                            pathKey, lastPathSegment, questionWithPath);
                } else {
                    log.warn("AI生成的推荐问题为空，pathKey: {}", pathKey);
                }
            } catch (Exception e) {
                log.error("异步生成推荐问题失败，pathKey: {}", pathKey, e);
            }
        });
    }

    /**
     * 构建缓存key
     *
     * @param userId        用户ID
     * @param simpleProfile 简单画像
     * @param pathKey       标签路径
     * @return 缓存key
     */
    private String buildCacheKey(Long userId, String simpleProfile, String pathKey) {
        return RECOMMENDATION_CACHE_PREFIX + userId + "::" + simpleProfile + "::" + pathKey;
    }

}
