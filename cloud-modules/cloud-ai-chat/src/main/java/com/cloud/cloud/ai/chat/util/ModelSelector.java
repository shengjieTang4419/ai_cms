package com.cloud.cloud.ai.chat.util;

import com.cloud.cloud.ai.chat.dto.ChatContext;
import com.cloud.cloud.ai.chat.provider.ModelProvider;
import com.cloud.cloud.ai.chat.provider.ModelProviderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型选择器 - 根据请求内容选择合适的模型（SPI版本）
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 智能模型选择工具 - 基于SPI
 * @date 2025/10/14
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelSelector {

    private final ModelProviderManager providerManager;

    /**
     * 根据聊天上下文选择合适的模型
     * 优先级：OCR处理过的图片 > 文本模型
     */
    public ModelProvider selectModelProvider(ChatContext context) {
        // 当前策略：即使有图片，如果已经通过OCR提取了文字，也使用文本模型
        // 因为图片内容已经转化为文字，不需要使用视觉模型
        ModelProvider provider = providerManager.getDefaultProvider();
        log.info("根据上下文选择模型: {} (hasImages: {}, ragEnhanced: {})", 
                provider.getModelName(), context.hasImages(), context.isRagEnhanced());
        return provider;
    }

    /**
     * 根据是否有图片选择模型（支持多张图片）
     * @deprecated 使用 selectModelProvider(ChatContext) 替代
     */
    @Deprecated
    public String selectModel(List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            ModelProvider visionProvider = providerManager.getProviderByCapability(true);
            log.info("检测到{}张图片，选择Vision模型: {}", imageUrls.size(), visionProvider.getModelName());
            return visionProvider.getModelName();
        }
        ModelProvider defaultProvider = providerManager.getDefaultProvider();
        log.info("无图片输入，使用默认文本模型: {}", defaultProvider.getModelName());
        return defaultProvider.getModelName();
    }

    /**
     * 获取ModelProvider实例
     */
    public ModelProvider getProvider(String modelName) {
        return providerManager.getProvider(modelName);
    }

    /**
     * 获取ProviderManager实例（用于其他Service）
     */
    public ModelProviderManager getProviderManager() {
        return providerManager;
    }
}

