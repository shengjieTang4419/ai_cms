package com.cloud.ai.chat.controller;


import com.cloud.ai.chat.domain.UserTags;
import com.cloud.ai.chat.service.impl.ChatMessageService;
import com.cloud.ai.chat.service.impl.RecommendationService;
import com.cloud.ai.chat.service.impl.UserProfileService;
import com.cloud.ai.chat.util.ValidationUtils;
import com.cloud.common.core.domain.R;
import com.cloud.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐控制器 - 传统REST API
 * @date 2025/01/16 11:00
 */
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final UserProfileService userProfileService;
    private final RecommendationService recommendationService;
    private final ChatMessageService chatMessageService;

    /**
     * 获取用户个性化推荐（首页使用）- 基于AI生成推荐提问
     */
    @GetMapping("/personalized")
    public R<Map<String, Object>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "5") int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("获取用户个性化推荐（AI生成），limit: {}", limit);

        try {
            // 验证参数
            ValidationUtils.validateRecommendationLimit(limit);
            // 调用推荐服务生成个性化推荐
            Map<String, Object> recommendations = recommendationService.generatePersonalizedRecommendations(userId, limit);
            return R.ok(recommendations);
        } catch (Exception e) {
            log.error("获取个性化推荐失败", e);
            return R.fail("获取推荐失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户兴趣分析
     */
    @GetMapping("/interests")
    public R<Map<String, Object>> getUserInterests() {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("获取用户兴趣分析，userId: {}", userId);

        try {
            List<UserTags> allTags = userProfileService.getUserTags(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("totalTags", allTags.size());
            response.put("tags", allTags);

            return R.ok(response);

        } catch (Exception e) {
            log.error("获取用户兴趣分析失败", e);
            return R.fail("获取兴趣分析失败：" + e.getMessage());
        }
    }


    @GetMapping("/recommendations")
    public R<List<String>> getUserInterests(@RequestParam String sessionId, @RequestParam String dialogueId) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            List<String> recommendationMessage = chatMessageService.getRecommendationMessage(sessionId, dialogueId, userId);
            return R.ok(recommendationMessage);
        } catch (Exception e) {
            log.error("获取话题引导失败", e);
            return R.fail("获取话题引导失败");
        }
    }

}
