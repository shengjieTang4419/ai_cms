package com.cloud.cloud.ai.chat.provider.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.OcrOptions;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.cloud.cloud.ai.chat.domain.Image;
import com.cloud.cloud.ai.chat.domain.ImageTaskType;
import com.cloud.cloud.ai.chat.provider.OcrProvider;
import com.cloud.cloud.ai.chat.repository.ImageRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云 DashScope OCR Provider 实现
 * 使用阿里云通义千问视觉模型进行OCR和图像识别
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @description: DashScope OCR Provider 实现
 * @date 2025/11/28
 */
@Component
@ConditionalOnProperty(
    prefix = "ai.provider.ocr.dashscope",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true  // 默认启用
)
@RequiredArgsConstructor
@Slf4j
public class DashScopeOcrProvider implements OcrProvider {
    
    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;
    
    @Value("${ai.provider.ocr.dashscope.model:qwen-vl-ocr}")
    private String modelName;
    
    @Value("${ai.provider.ocr.dashscope.priority:1}")
    private int priority;
    
    @Value("${minio.bucket-name:images}")
    private String bucketName;
    
    private final ImageRepository imageRepository;
    private final MinioClient minioClient;
    
    @Override
    public String getProviderName() {
        return "dashscope";
    }
    
    @Override
    public String getDisplayName() {
        return "阿里云通义";
    }
    
    @Override
    public boolean supportsTableRecognition() {
        return true;
    }
    
    @Override
    public boolean supportsHandwritingRecognition() {
        return true;
    }
    
    @Override
    public OcrResult extractText(String imageId, String imageUrl) {
        return analyzeImage(imageId, imageUrl, ImageTaskType.OCR, null);
    }
    
    @Override
    public OcrResult analyzeImage(String imageId, String imageUrl, 
                                  ImageTaskType taskType, String customPrompt) {
        try {
            String taskName = taskType == ImageTaskType.OCR ? "OCR提取文字" :
                    taskType == ImageTaskType.IMAGE_RECOGNITION ? "图像识别" : "图像分析";
            log.info("[DashScope OCR] 开始{}，imageId: {}, taskType: {}", taskName, imageId, taskType);
            
            // 检查URL是否是localhost，如果是则从MinIO读取文件并转换为base64
            String imageContent = prepareImageContent(imageId, imageUrl);
            log.debug("[DashScope OCR] 准备图片内容完成，使用base64: {}", 
                imageContent != null && imageContent.startsWith("data:"));
            
            // 确定使用的提示词
            String prompt;
            if (taskType == ImageTaskType.CUSTOM && customPrompt != null && !customPrompt.trim().isEmpty()) {
                prompt = customPrompt.trim();
            } else {
                prompt = taskType.getDefaultPrompt();
                if (prompt == null) {
                    prompt = "请分析这张图片的内容。";
                }
            }
            
            MultiModalConversation conv = new MultiModalConversation();
            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("image", imageContent != null ? imageContent : imageUrl);
            imageMap.put("max_pixels", "6422528");
            imageMap.put("min_pixels", "3136");
            imageMap.put("enable_rotate", true);
            
            MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(imageMap, Collections.singletonMap("text", prompt)))
                .build();
            
            // 构建参数
            MultiModalConversationParam param;
            if (taskType == ImageTaskType.OCR) {
                // OCR任务需要添加OCR选项
                OcrOptions ocrOptions = OcrOptions.builder()
                    .task(OcrOptions.Task.ADVANCED_RECOGNITION)
                    .build();
                param = MultiModalConversationParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(modelName)
                    .message(userMessage)
                    .ocrOptions(ocrOptions)
                    .build();
            } else {
                // 图像识别任务不需要OCR选项
                param = MultiModalConversationParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(modelName)
                    .message(userMessage)
                    .build();
            }
            
            MultiModalConversationResult result = conv.call(param);
            
            if (result.getOutput() != null && result.getOutput().getChoices() != null &&
                    !result.getOutput().getChoices().isEmpty()) {
                
                String resultText = (String) result.getOutput().getChoices().get(0)
                    .getMessage().getContent().get(0).get("text");
                
                // 更新图片信息
                updateImageInfo(imageId, resultText, taskType);
                
                log.info("✅ [DashScope OCR] {}成功，imageId: {}", taskName, imageId);
                return new OcrResult(true, resultText, "SUCCESS");
            } else {
                log.warn("[DashScope OCR] {}返回结果为空，imageId: {}", taskName, imageId);
                updateOCRStatus(imageId, "FAILED");
                return new OcrResult(false, null, "FAILED");
            }
            
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            log.error("❌ [DashScope OCR] 识别失败，imageId: {}", imageId, e);
            updateOCRStatus(imageId, "FAILED");
            return new OcrResult(false, null, "FAILED: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ [DashScope OCR] 异常，imageId: {}", imageId, e);
            updateOCRStatus(imageId, "FAILED");
            return new OcrResult(false, null, "FAILED: " + e.getMessage());
        }
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    /**
     * 更新图片信息
     */
    private void updateImageInfo(String imageId, String resultText, ImageTaskType taskType) {
        imageRepository.findById(imageId).ifPresent(image -> {
            if (ImageTaskType.OCR.equals(taskType)) {
                image.setOcrText(resultText);
                image.setOcrStatus("SUCCESS");
            } else if (ImageTaskType.IMAGE_RECOGNITION.equals(taskType)) {
                image.setImageParsingContext(resultText);
                image.setParsingStatus("SUCCESS");
            }
            imageRepository.save(image);
            log.debug("图片解析成功，imageId: {}", imageId);
        });
    }
    
    /**
     * 更新OCR状态
     */
    private void updateOCRStatus(String imageId, String status) {
        imageRepository.findById(imageId).ifPresent(image -> {
            image.setOcrStatus(status);
            imageRepository.save(image);
        });
    }
    
    /**
     * 准备图片内容
     * 如果URL是localhost，从MinIO读取文件并转换为base64格式
     */
    private String prepareImageContent(String imageId, String imageUrl) {
        try {
            // 检查是否是localhost URL
            if (!isLocalhostUrl(imageUrl)) {
                return null; // 不是localhost，返回null表示使用原URL
            }
            
            log.debug("检测到localhost URL，从MinIO读取文件并转换为base64");
            
            // 从数据库获取Image对象
            Image image = imageRepository.findById(imageId).orElse(null);
            if (image == null || image.getFilePath() == null) {
                log.warn("无法找到图片记录或filePath为空，imageId: {}，使用原URL", imageId);
                return null;
            }
            
            String filePath = image.getFilePath();
            String contentType = getContentType(image, filePath);
            
            // 从MinIO读取文件
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(filePath).build())) {
                
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int nRead;
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                byte[] imageBytes = buffer.toByteArray();
                
                // 转换为base64
                String base64Data = java.util.Base64.getEncoder().encodeToString(imageBytes);
                String base64Content = "data:" + contentType + ";base64," + base64Data;
                
                log.debug("成功将图片转换为base64，imageId: {}, 大小: {} bytes", imageId, imageBytes.length);
                return base64Content;
            }
            
        } catch (Exception e) {
            log.error("准备图片内容时发生异常，imageId: {}", imageId, e);
            return null; // 出错时返回null，使用原URL
        }
    }
    
    /**
     * 获取内容类型
     */
    @NotNull
    private String getContentType(Image image, String filePath) {
        String contentType = image.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            // 根据文件扩展名推断contentType
            if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filePath.endsWith(".png")) {
                contentType = "image/png";
            } else if (filePath.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filePath.endsWith(".webp")) {
                contentType = "image/webp";
            } else {
                contentType = "image/png"; // 默认
            }
        }
        return contentType;
    }
    
    /**
     * 检查URL是否是localhost或内网地址
     */
    private boolean isLocalhostUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost().toLowerCase();
            return host.equals("localhost") ||
                    host.equals("127.0.0.1") ||
                    host.equals("0.0.0.0");
        } catch (Exception e) {
            log.warn("解析URL失败: {}", url, e);
            return false;
        }
    }
}
