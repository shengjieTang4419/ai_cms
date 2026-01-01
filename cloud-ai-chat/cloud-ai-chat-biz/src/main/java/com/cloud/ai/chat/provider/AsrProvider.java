package com.cloud.ai.chat.provider;

import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别 Provider 接口
 * 所有 ASR 服务提供商需要实现此接口
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: ASR Provider SPI 接口
 * @date 2025/11/28
 */
public interface AsrProvider {

    /**
     * 获取提供商名称（唯一标识）
     *
     * @return 例如：dashscope、baidu、tencent、xunfei
     */
    String getProviderName();

    /**
     * 获取提供商显示名称
     *
     * @return 例如：阿里云通义、百度语音、腾讯云
     */
    String getDisplayName();

    /**
     * 获取支持的音频格式
     *
     * @return 支持的格式列表，例如：["wav", "mp3", "pcm"]
     */
    String[] getSupportedFormats();

    /**
     * 语音识别
     *
     * @param audioFile 音频文件
     * @return 识别结果文本
     */
    String recognizeAudio(MultipartFile audioFile);

    /**
     * 是否启用此提供商
     *
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 获取优先级（数字越小优先级越高）
     *
     * @return 优先级，默认为10
     */
    default int getPriority() {
        return 10;
    }
}
