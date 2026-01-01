package com.cloud.ai.chat.provider.impl;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.cloud.ai.chat.provider.AsrProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云 DashScope ASR Provider 实现
 * 使用阿里云通义千问语音识别服务
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: DashScope ASR Provider 实现
 * @date 2025/11/28
 */
@Component
@ConditionalOnProperty(
        prefix = "ai.provider.asr.dashscope",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true  // 默认启用
)
@Slf4j
public class DashScopeAsrProvider implements AsrProvider {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${ai.provider.asr.dashscope.model:fun-asr-realtime}")
    private String model;

    @Value("${ai.provider.asr.dashscope.sample-rate:16000}")
    private int sampleRate;

    @Value("${ai.provider.asr.dashscope.priority:1}")
    private int priority;

    @Override
    public String getProviderName() {
        return "dashscope";
    }

    @Override
    public String getDisplayName() {
        return "阿里云通义";
    }

    @Override
    public String[] getSupportedFormats() {
        // DashScope fun-asr-realtime 官方支持的格式
        // 参考：https://help.aliyun.com/zh/model-studio/fun-asr-realtime-java-sdk
        return new String[]{"pcm", "wav", "mp3", "opus", "speex", "aac", "amr"};
    }

    @Override
    public String recognizeAudio(MultipartFile audioFile) {
        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = createTempFile(audioFile);

            // 创建Recognition实例
            Recognition recognizer = new Recognition();

            // 检测音频格式
            String format = detectAudioFormat(audioFile.getOriginalFilename());

            // 创建RecognitionParam
            RecognitionParam param = RecognitionParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .format(format)
                    .sampleRate(sampleRate)
                    .parameter("language_hints", new String[]{"zh", "en"})
                    .build();

            // 执行识别
            String result = recognizer.call(param, tempFile);

            // 记录请求指标
            log.info("[DashScope ASR] requestId: {}, first delay: {}ms, last delay: {}ms",
                    recognizer.getLastRequestId(),
                    recognizer.getFirstPackageDelay(),
                    recognizer.getLastPackageDelay());

            // 解析 JSON 响应，提取 text 字段
            String recognizedText = extractTextFromJson(result);
            log.info("✅ [DashScope ASR] 识别成功，结果: {}", recognizedText);

            return recognizedText;

        } catch (Exception e) {
            log.error("❌ [DashScope ASR] 识别失败", e);
            throw new RuntimeException("DashScope 语音识别失败：" + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    log.debug("临时文件已删除: {}", tempFile.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("临时文件删除失败: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * 创建临时文件
     */
    private File createTempFile(MultipartFile audioFile) throws IOException {
        String originalFilename = audioFile.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".wav";

        Path tempFilePath = Files.createTempFile("asr_", suffix);
        Files.copy(audioFile.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        log.debug("创建临时文件: {}", tempFilePath);
        return tempFilePath.toFile();
    }

    /**
     * 从 JSON 响应中提取 text 字段
     * 响应格式：{"sentences":[{"text":"今天天气怎么样？",...},...]}
     */
    private String extractTextFromJson(String jsonResult) {
        if (jsonResult == null || jsonResult.trim().isEmpty()) {
            log.warn("ASR 响应为空");
            return "";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResult);

            // 提取所有 sentences 中的 text 并拼接
            List<String> texts = new ArrayList<>();
            JsonNode sentences = root.get("sentences");

            if (sentences != null && sentences.isArray()) {
                for (JsonNode sentence : sentences) {
                    JsonNode textNode = sentence.get("text");
                    if (textNode != null && !textNode.asText().isEmpty()) {
                        texts.add(textNode.asText());
                    }
                }
            }

            // 拼接所有文本（如果有多句）
            String result = String.join("", texts);
            log.debug("从 JSON 中提取文本: {}", result);
            return result;

        } catch (Exception e) {
            log.error("解析 ASR JSON 响应失败: {}", jsonResult, e);
            // 如果 JSON 解析失败，尝试直接返回原始文本
            return jsonResult != null ? jsonResult.trim() : "";
        }
    }

    /**
     * 检测音频格式
     * 严格按照阿里云官方文档，仅支持 7 种格式：pcm、wav、mp3、opus、speex、aac、amr
     * 参考：https://help.aliyun.com/zh/model-studio/fun-asr-realtime-java-sdk
     */
    private String detectAudioFormat(String filename) {
        if (filename == null) {
            log.warn("文件名为空，使用默认格式 wav");
            return "wav";
        }

        String lowerFilename = filename.toLowerCase();

        // 阿里云官方支持的 7 种格式
        if (lowerFilename.endsWith(".pcm")) return "pcm";
        if (lowerFilename.endsWith(".wav")) return "wav";
        if (lowerFilename.endsWith(".mp3")) return "mp3";
        if (lowerFilename.endsWith(".opus")) return "opus";
        if (lowerFilename.endsWith(".speex")) return "speex";
        if (lowerFilename.endsWith(".aac")) return "aac";
        if (lowerFilename.endsWith(".amr")) return "amr";

        // 不支持的格式
        log.error("不支持的音频格式: {}，阿里云仅支持 pcm/wav/mp3/opus/speex/aac/amr", filename);
        throw new IllegalArgumentException("不支持的音频格式: " + filename +
                "，仅支持：pcm、wav、mp3、opus、speex、aac、amr");
    }
}
