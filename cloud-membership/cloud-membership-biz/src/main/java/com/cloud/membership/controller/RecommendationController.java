package com.cloud.membership.controller;


import com.cloud.common.core.domain.R;
import com.cloud.common.security.SecurityUtils;
import com.cloud.membership.service.impl.RecommendationService;
import com.cloud.membership.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性推荐
 * @date 2026/2/22 13:41
 */
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 获取用户个性化推荐（首页使用）- 基于AI生成推荐提问
     */
    @GetMapping("/personalized")
    public R<List<String>> getPersonalizedRecommendations(@RequestParam(defaultValue = "5") int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("获取用户个性化推荐（AI生成），limit: {}", limit);
        try {
            // 验证参数
            ValidationUtils.validateRecommendationLimit(limit);
            // 调用推荐服务生成个性化推荐
            List<String> recommendations = recommendationService.lazyGeneratePersonalRecommendations(userId, limit);
            return R.ok(recommendations);
        } catch (Exception e) {
            log.error("获取个性化推荐失败", e);
            return R.fail("获取推荐失败：" + e.getMessage());
        }
    }
}
