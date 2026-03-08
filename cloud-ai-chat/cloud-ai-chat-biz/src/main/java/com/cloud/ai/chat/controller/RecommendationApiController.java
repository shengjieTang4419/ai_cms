package com.cloud.ai.chat.controller;

import com.cloud.ai.chat.service.impl.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐API控制器 - 供OpenFeign调用
 * @date 2026/2/21 15:49
 */
@RestController
@RequestMapping("/api/ai/recommendation")
@RequiredArgsConstructor
@Slf4j
public class RecommendationApiController {

    private final RecommendationService recommendationService;

    /**
     * 根据用户画像和标签生成个性化推荐问题
     * 供OpenFeign调用
     *
     * @param userProfile 用户简单画像
     * @param tagName     兴趣标签
     * @return 生成的推荐问题
     */
    @PostMapping("/generateQuestion")
    public String generateQuestionsFromTags(@RequestParam("userProfile") String userProfile,
                                            @RequestParam("tagName") String tagName) {
        log.info("OpenFeign调用生成推荐问题，userProfile: {}, tagName: {}", userProfile, tagName);
        return recommendationService.generateQuestionsFromTags(userProfile, tagName);
    }
}
