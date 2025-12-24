package com.cloud.ai.chat.manager;

import com.cloud.ai.chat.provider.OcrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * OCR Provider 管理器
 * 自动发现并管理所有 OCR Provider
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: OCR Provider 管理器，负责Provider的注册、发现和选择
 * @date 2025/11/28
 */
@Component
@Slf4j
public class OcrProviderManager {

    private final List<OcrProvider> providers;
    private final OcrProvider defaultProvider;

    /**
     * 构造函数，Spring 会自动注入所有 OcrProvider 实现
     */
    public OcrProviderManager(List<OcrProvider> providers) {
        this.providers = providers;

        // 按优先级排序，选择最高优先级的启用的Provider
        this.defaultProvider = providers.stream()
                .filter(OcrProvider::isEnabled)
                .min(Comparator.comparingInt(OcrProvider::getPriority))
                .orElseThrow(() -> new IllegalStateException("未找到可用的 OCR Provider"));

        log.info("✅ 加载了 {} 个 OCR Provider，默认使用: {} ({})",
                providers.size(),
                defaultProvider.getDisplayName(),
                defaultProvider.getProviderName());

        // 输出所有Provider信息
        providers.forEach(provider ->
                log.info("  - {} ({}) - 优先级: {}, 启用: {}, 支持表格: {}, 支持手写: {}",
                        provider.getDisplayName(),
                        provider.getProviderName(),
                        provider.getPriority(),
                        provider.isEnabled(),
                        provider.supportsTableRecognition(),
                        provider.supportsHandwritingRecognition())
        );
    }

    /**
     * 获取默认 Provider
     *
     * @return 默认的 OCR Provider
     */
    public OcrProvider getDefaultProvider() {
        return defaultProvider;
    }

    /**
     * 根据名称获取 Provider
     *
     * @param providerName Provider名称
     * @return 指定的Provider，如果未找到或未启用则返回默认Provider
     */
    public OcrProvider getProvider(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            return defaultProvider;
        }

        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName.trim()))
                .filter(OcrProvider::isEnabled)
                .findFirst()
                .orElseGet(() -> {
                    log.warn("未找到名为 '{}' 的启用的 OCR Provider，使用默认Provider", providerName);
                    return defaultProvider;
                });
    }

    /**
     * 获取所有启用的 Provider
     *
     * @return 所有启用的Provider列表，按优先级排序
     */
    public List<OcrProvider> getAllProviders() {
        return providers.stream()
                .filter(OcrProvider::isEnabled)
                .sorted(Comparator.comparingInt(OcrProvider::getPriority))
                .toList();
    }

    /**
     * 获取支持表格识别的Provider
     *
     * @return 支持表格识别的Provider，如果没有则返回默认Provider
     */
    public OcrProvider getTableRecognitionProvider() {
        return providers.stream()
                .filter(OcrProvider::isEnabled)
                .filter(OcrProvider::supportsTableRecognition)
                .min(Comparator.comparingInt(OcrProvider::getPriority))
                .orElse(defaultProvider);
    }

    /**
     * 获取支持手写识别的Provider
     *
     * @return 支持手写识别的Provider，如果没有则返回默认Provider
     */
    public OcrProvider getHandwritingRecognitionProvider() {
        return providers.stream()
                .filter(OcrProvider::isEnabled)
                .filter(OcrProvider::supportsHandwritingRecognition)
                .min(Comparator.comparingInt(OcrProvider::getPriority))
                .orElse(defaultProvider);
    }
}
