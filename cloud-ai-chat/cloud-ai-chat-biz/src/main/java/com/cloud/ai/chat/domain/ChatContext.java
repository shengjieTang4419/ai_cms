package com.cloud.ai.chat.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 聊天上下文 - 用于模型选择和请求处理
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 封装聊天请求的所有参数，用于优雅地传递上下文信息
 * @date 2025/10/30
 */
@Data
@Builder
public class ChatContext {

    /**
     * 用户查询（原始问题）
     */
    private String query;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 是否使用RAG增强
     */
    private boolean ragEnhanced;

    /**
     * 是否启用全网搜索
     */
    private boolean withEnableSearch;

    /**
     * 原始查询（用于记录，可能与query不同，比如RAG增强后）
     */
    private String originQuery;

    /**
     * 图片URL列表
     */
    private List<String> imageUrls;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否使用Thinking模型（深度思考）
     */
    private boolean useThinking;

    /**
     * 当前位置经度（用于路线规划）
     */
    private String longitude;

    /**
     * 当前位置纬度（用于路线规划）
     */
    private String latitude;

    /**
     * 是否包含图片
     */
    public boolean hasImages() {
        return imageUrls != null && !imageUrls.isEmpty();
    }

    /**
     * 是否有位置信息
     */
    public boolean hasLocation() {
        return longitude != null && !longitude.isEmpty() && latitude != null && !latitude.isEmpty();
    }

    /**
     * 获取位置坐标字符串（格式：经度,纬度）
     */
    public String getLocationCoordinate() {
        if (hasLocation()) {
            return longitude + "," + latitude;
        }
        return null;
    }

    /**
     * 获取图片数量
     */
    public int getImageCount() {
        return imageUrls == null ? 0 : imageUrls.size();
    }
}



