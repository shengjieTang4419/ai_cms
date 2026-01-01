package com.cloud.ai.chat.controller;

import com.cloud.ai.chat.service.impl.AsrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 语音识别控制器
 * <p>
 * 提供语音转文字接口
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: ASR语音识别API接口
 * @date 2025/01/16
 */
@RestController
@RequestMapping("/api/asr")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "语音识别", description = "语音转文字API")
public class AsrController {

    private final AsrService asrService;

    /**
     * 语音转文字接口
     * <p>
     * 支持的格式（严格按照阿里云官方文档）：
     * pcm、wav、mp3、opus、speex、aac、amr
     * 参考：https://help.aliyun.com/zh/model-studio/fun-asr-realtime-java-sdk
     *
     * @param audioFile 音频文件
     * @return 识别出的文本内容
     */
    @PostMapping("/recognize")
    @Operation(summary = "语音转文字", description = "上传音频文件，返回识别的文本内容")
    public ResponseEntity<Map<String, Object>> recognizeAudio(@RequestParam("file") MultipartFile audioFile) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证文件
            if (audioFile == null || audioFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "音频文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 验证文件大小（限制10MB）
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (audioFile.getSize() > maxSize) {
                response.put("success", false);
                response.put("message", "音频文件大小不能超过10MB");
                return ResponseEntity.badRequest().body(response);
            }

            // 验证文件类型（严格按照阿里云官方文档）
            String originalFilename = audioFile.getOriginalFilename();
            if (!isValidAudioFile(originalFilename)) {
                response.put("success", false);
                response.put("message", "不支持的音频格式！仅支持：pcm、wav、mp3、opus、speex、aac、amr");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("收到语音识别请求，文件名: {}, 大小: {} bytes", originalFilename, audioFile.getSize());

            // 执行语音识别
            String recognizedText = asrService.recognizeAudio(audioFile);

            if (recognizedText == null || recognizedText.isEmpty()) {
                response.put("success", false);
                response.put("message", "语音识别失败，未识别到内容");
                return ResponseEntity.ok(response);
            }

            log.info("语音识别成功，文本长度: {} 字符", recognizedText.length());
            response.put("success", true);
            response.put("text", recognizedText);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("语音识别失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("语音识别处理异常", e);
            response.put("success", false);
            response.put("message", "语音识别失败，请稍后重试");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 验证是否为有效的音频文件
     * 严格按照阿里云 fun-asr-realtime 官方文档
     * 仅支持 7 种格式：pcm、wav、mp3、opus、speex、aac、amr
     * 参考：https://help.aliyun.com/zh/model-studio/fun-asr-realtime-java-sdk
     */
    private boolean isValidAudioFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".pcm")
                || lowerFilename.endsWith(".wav")
                || lowerFilename.endsWith(".mp3")
                || lowerFilename.endsWith(".opus")
                || lowerFilename.endsWith(".speex")
                || lowerFilename.endsWith(".aac")
                || lowerFilename.endsWith(".amr");
    }
}
