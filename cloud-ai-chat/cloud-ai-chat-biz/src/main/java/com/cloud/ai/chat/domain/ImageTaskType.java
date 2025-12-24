package com.cloud.ai.chat.domain;


/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 图片识别枚举
 * @date 2025/10/30 11:00
 */
public enum ImageTaskType {

    /**
     * OCR文字识别：提取图片中的文字内容
     */
    OCR("请提取图片中的所有文字内容，保持原有格式。"),
    /**
     * 图像识别：识别图片中的内容、场景、物体等
     */
    IMAGE_RECOGNITION("请详细描述这张图片中的内容，包括场景、物体、人物、动作等。"),
    /**
     * 自定义：使用自定义的提示词
     */
    CUSTOM(null);

    private final String defaultPrompt;

    ImageTaskType(String defaultPrompt) {
        this.defaultPrompt = defaultPrompt;
    }

    public String getDefaultPrompt() {
        return defaultPrompt;
    }
}
