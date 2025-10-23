package com.cloud.cloud.ai.chat.controller;

import com.cloud.cloud.ai.chat.domain.UserTags;
import com.cloud.cloud.ai.chat.service.RecommendationService;
import com.cloud.cloud.ai.chat.service.UserProfileService;
import com.cloud.cloud.ai.chat.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final UserProfileService userProfileService;
    private final RecommendationService recommendationService;

    /**
     * 获取用户个性化推荐（首页使用）- 基于AI生成推荐提问
     */
    @GetMapping("/personalized/{userId}")
    public ResponseEntity<Map<String, Object>> getPersonalizedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") int limit) {

        log.info("获取用户个性化推荐（AI生成），userId: {}, limit: {}", userId, limit);

        try {
            // 验证参数
            ValidationUtils.validateRecommendationLimit(limit);

            // 调用推荐服务生成个性化推荐
            Map<String, Object> recommendations = recommendationService.generatePersonalizedRecommendations(userId, limit);

            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            log.error("获取个性化推荐失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取推荐失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取用户兴趣分析
     */
    @GetMapping("/interests/{userId}")
    public ResponseEntity<Map<String, Object>> getUserInterests(@PathVariable Long userId) {
        
        log.info("获取用户兴趣分析，userId: {}", userId);
        
        try {
            List<UserTags> allTags = userProfileService.getUserTags(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("totalTags", allTags.size());
            response.put("tags", allTags);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户兴趣分析失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取兴趣分析失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
