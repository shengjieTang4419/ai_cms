package com.cloud.cloud.ai.chat.util;

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
     * 根据是否有图片选择模型（支持多张图片）
     */
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

