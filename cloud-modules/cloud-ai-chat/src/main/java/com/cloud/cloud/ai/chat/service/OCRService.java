package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.domain.ImageTaskType;
import com.cloud.cloud.ai.chat.manager.OcrProviderManager;
import com.cloud.cloud.ai.chat.provider.OcrProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 图像分析服务 - 支持OCR文字识别和图像识别
 * <p>
 * 通过 Provider 管理器调用不同的OCR服务提供商
 * 不再直接调用SDK，而是通过 Provider 接口实现解耦
 *
 * @author shengjie.tang
 * @version 2.0.0
 * @description: OCR服务，支持多种OCR提供商
 * @date 2025/11/28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OCRService {

    private final OcrProviderManager providerManager;

    /**
     * 同步OCR提取文字（使用默认Provider）
     *
     * @param imageId  图片ID
     * @param imageUrl 图片URL
     * @return OCR结果（包含文字和状态）
     */
    public OCRResult extractTextSync(String imageId, String imageUrl) {
        return extractTextSync(imageId, imageUrl, null);
    }
    
    /**
     * 同步OCR提取文字（指定Provider）
     *
     * @param imageId  图片ID
     * @param imageUrl 图片URL
     * @param providerName 指定的Provider名称
     * @return OCR结果（包含文字和状态）
     */
    public OCRResult extractTextSync(String imageId, String imageUrl, String providerName) {
        OcrProvider provider = providerName != null
            ? providerManager.getProvider(providerName)
            : providerManager.getDefaultProvider();
        
        log.info("使用 {} 进行 OCR 文字提取", provider.getDisplayName());
        
        try {
            OcrProvider.OcrResult result = provider.extractText(imageId, imageUrl);
            return new OCRResult(result.isSuccess(), result.getText(), result.getStatus());
        } catch (Exception e) {
            log.error("OCR提取失败，Provider: {}", provider.getProviderName(), e);
            return new OCRResult(false, null, "FAILED: " + e.getMessage());
        }
    }

    /**
     * 同步图像识别（使用默认Provider）
     *
     * @param imageId  图片ID
     * @param imageUrl 图片URL
     * @return 图像识别结果（包含描述文字和状态）
     */
    public OCRResult analyzeImageSync(String imageId, String imageUrl) {
        return analyzeImageSync(imageId, imageUrl, ImageTaskType.IMAGE_RECOGNITION, null);
    }

    /**
     * 同步图像分析（支持OCR、图像识别或自定义提示词）
     *
     * @param imageId      图片ID
     * @param imageUrl     图片URL
     * @param taskType     任务类型：OCR、图像识别或自定义
     * @param customPrompt 自定义提示词（仅当taskType为CUSTOM时使用）
     * @return 分析结果（包含文字和状态）
     */
    public OCRResult analyzeImageSync(String imageId, String imageUrl, 
                                     ImageTaskType taskType, String customPrompt) {
        return analyzeImageSync(imageId, imageUrl, taskType, customPrompt, null);
    }
    
    /**
     * 同步图像分析（指定Provider）
     *
     * @param imageId      图片ID
     * @param imageUrl     图片URL
     * @param taskType     任务类型：OCR、图像识别或自定义
     * @param customPrompt 自定义提示词
     * @param providerName 指定的Provider名称
     * @return 分析结果（包含文字和状态）
     */
    public OCRResult analyzeImageSync(String imageId, String imageUrl,
                                     ImageTaskType taskType, String customPrompt,
                                     String providerName) {
        OcrProvider provider = providerName != null
            ? providerManager.getProvider(providerName)
            : providerManager.getDefaultProvider();
        
        String taskName = taskType == ImageTaskType.OCR ? "OCR提取" :
                taskType == ImageTaskType.IMAGE_RECOGNITION ? "图像识别" : "图像分析";
        log.info("使用 {} 进行{}", provider.getDisplayName(), taskName);
        
        try {
            OcrProvider.OcrResult result = provider.analyzeImage(imageId, imageUrl, taskType, customPrompt);
            return new OCRResult(result.isSuccess(), result.getText(), result.getStatus());
        } catch (Exception e) {
            log.error("{}失败，Provider: {}", taskName, provider.getProviderName(), e);
            return new OCRResult(false, null, "FAILED: " + e.getMessage());
        }
    }

    /**
     * 获取所有可用的Provider
     *
     * @return Provider列表
     */
    public java.util.List<String> getAvailableProviders() {
        return providerManager.getAllProviders().stream()
            .map(OcrProvider::getProviderName)
            .toList();
    }
    
    /**
     * OCR结果封装类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class OCRResult {
        private boolean success;
        private String text;
        private String status;
    }
}

