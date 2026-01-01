package com.cloud.ai.chat.provider;

import org.springframework.ai.chat.client.ChatClient;

/**
 * 模型提供者接口 - SPI机制
 * <p>
 * 每个AI模型需要实现此接口，系统会自动发现并加载所有实现
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: SPI接口定义
 * @date 2025/10/14
 */
public interface ModelProvider {

    /**
     * 获取模型名称（唯一标识）
     */
    String getModelName();

    /**
     * 获取模型显示名称
     */
    String getDisplayName();

    /**
     * 是否支持Vision（图片输入）
     */
    boolean supportsVision();

    /**
     * 是否支持Stream（流式输出）
     */
    boolean supportsStream();

    /**
     * 是否支持Thinking模型（深度思考）
     */
    default boolean supportsThinking() {
        return false;
    }

    /**
     * 获取ChatClient实例
     */
    ChatClient getChatClient();

    /**
     * 获取模型优先级（数字越小优先级越高）
     * 默认返回10，Vision模型可以返回5
     */
    default int getPriority() {
        return 10;
    }

    /**
     * 是否启用此模型
     * 可以通过配置文件控制
     */
    default boolean isEnabled() {
        return true;
    }
}

