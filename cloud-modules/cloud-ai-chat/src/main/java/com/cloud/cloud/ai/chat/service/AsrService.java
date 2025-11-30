package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.manager.AsrProviderManager;
import com.cloud.cloud.ai.chat.provider.AsrProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别服务
 * <p>
 * 通过 Provider 管理器调用不同的语音识别服务提供商
 * 不再直接调用SDK，而是通过 Provider 接口实现解耦
 *
 * @author shengjie.tang
 * @version 2.0.0
 * @description: ASR（Automatic Speech Recognition）语音识别服务
 * @date 2025/11/28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsrService {

    private final AsrProviderManager providerManager;

    /**
     * 识别音频文件中的语音内容（使用默认Provider）
     *
     * @param audioFile 音频文件（MultipartFile）
     * @return 识别出的文本内容
     */
    public String recognizeAudio(MultipartFile audioFile) {
        return recognizeAudio(audioFile, null);
    }
    
    /**
     * 识别音频文件中的语音内容（指定Provider）
     *
     * @param audioFile 音频文件（MultipartFile）
     * @param providerName 指定的Provider名称，null则使用默认Provider
     * @return 识别出的文本内容
     */
    public String recognizeAudio(MultipartFile audioFile, String providerName) {
        AsrProvider provider = providerName != null 
            ? providerManager.getProvider(providerName)
            : providerManager.getDefaultProvider();
        
        log.info("使用 {} 进行语音识别", provider.getDisplayName());
        
        try {
            return provider.recognizeAudio(audioFile);
        } catch (Exception e) {
            log.error("语音识别失败，Provider: {}", provider.getProviderName(), e);
            throw new RuntimeException("语音识别失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有可用的Provider
     *
     * @return Provider列表
     */
    public java.util.List<String> getAvailableProviders() {
        return providerManager.getAllProviders().stream()
            .map(AsrProvider::getProviderName)
            .toList();
    }
}
