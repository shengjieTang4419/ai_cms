package com.cloud.cloud.ai.chat.provider;

import com.cloud.cloud.ai.chat.domain.ImageTaskType;

/**
 * 文字识别 Provider 接口
 * 所有 OCR 服务提供商需要实现此接口
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @description: OCR Provider SPI 接口
 * @date 2025/11/28
 */
public interface OcrProvider {
    
    /**
     * 获取提供商名称（唯一标识）
     * @return 例如：dashscope、baidu、tencent
     */
    String getProviderName();
    
    /**
     * 获取提供商显示名称
     * @return 例如：阿里云通义、百度OCR、腾讯云
     */
    String getDisplayName();
    
    /**
     * 是否支持表格识别
     * @return 是否支持
     */
    boolean supportsTableRecognition();
    
    /**
     * 是否支持手写文字识别
     * @return 是否支持
     */
    boolean supportsHandwritingRecognition();
    
    /**
     * OCR 文字提取
     * @param imageId 图片ID
     * @param imageUrl 图片URL
     * @return OCR结果
     */
    OcrResult extractText(String imageId, String imageUrl);
    
    /**
     * 图像识别/分析
     * @param imageId 图片ID
     * @param imageUrl 图片URL
     * @param taskType 任务类型
     * @param customPrompt 自定义提示词
     * @return 识别结果
     */
    OcrResult analyzeImage(String imageId, String imageUrl, 
                          ImageTaskType taskType, String customPrompt);
    
    /**
     * 是否启用此提供商
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * 获取优先级（数字越小优先级越高）
     * @return 优先级，默认为10
     */
    default int getPriority() {
        return 10;
    }
    
    /**
     * OCR 结果封装
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    class OcrResult {
        private boolean success;
        private String text;
        private String status;
    }
}
