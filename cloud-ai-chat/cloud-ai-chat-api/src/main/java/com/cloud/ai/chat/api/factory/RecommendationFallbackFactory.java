package com.cloud.ai.chat.api.factory;

import com.cloud.ai.chat.api.RemoteRecommendationFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐服务降级工厂
 * @date 2026/2/21 15:49
 */
@Slf4j
@Component
public class RecommendationFallbackFactory implements FallbackFactory<RemoteRecommendationFeignService> {

    @Override
    public RemoteRecommendationFeignService create(Throwable cause) {
        log.error("个性化推荐服务调用失败，原因: {}", cause.getMessage());
        
        return new RemoteRecommendationFeignService() {
            @Override
            public String generateQuestionsFromTags(String userProfile, String tagName) {
                log.warn("使用降级方案生成推荐问题，tag: {}", tagName);
                return String.format("关于%s，你知道有哪些有趣的玩法？", tagName);
            }
        };
    }
}
