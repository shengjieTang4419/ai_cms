package com.cloud.ai.chat.api;

import com.cloud.ai.chat.api.factory.RecommendationFallbackFactory;
import com.cloud.common.core.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐远程服务接口
 * @date 2026/2/21 15:49
 */
@FeignClient(contextId = "remoteRecommendationFeignService",
        value = ServiceNameConstants.AI_CHAT_SERVICE,
        fallbackFactory = RecommendationFallbackFactory.class)
public interface RemoteRecommendationFeignService {

    /**
     * 根据用户画像和标签生成个性化推荐问题
     *
     * @param userProfile 用户简单画像
     * @param tagName     兴趣标签
     * @return 生成的推荐问题
     */
    @PostMapping("/api/ai/recommendation/generateQuestion")
    String generateQuestionsFromTags(@RequestParam("userProfile") String userProfile,
                                     @RequestParam("tagName") String tagName);
}
