package com.cloud.ai.chat.domain;

import io.jsonwebtoken.lang.Collections;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐响应对象
 * @date 2025/12/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 个性化推荐列表
     */
    private List<String> recommendations;

    public boolean isHaveValue() {
        return !Collections.isEmpty(this.getRecommendations());
    }
}
