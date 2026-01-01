package com.cloud.ai.chat.manager;

import com.cloud.ai.chat.provider.AsrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * ASR Provider 管理器
 * 自动发现并管理所有 ASR Provider
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: ASR Provider 管理器，负责Provider的注册、发现和选择
 * @date 2025/11/28
 */
@Component
@Slf4j
public class AsrProviderManager {

    private final List<AsrProvider> providers;
    private final AsrProvider defaultProvider;

    /**
     * 构造函数，Spring 会自动注入所有 AsrProvider 实现
     */
    public AsrProviderManager(List<AsrProvider> providers) {
        this.providers = providers;

        // 按优先级排序，选择最高优先级的启用的Provider
        this.defaultProvider = providers.stream()
                .filter(AsrProvider::isEnabled)
                .min(Comparator.comparingInt(AsrProvider::getPriority))
                .orElseThrow(() -> new IllegalStateException("未找到可用的 ASR Provider"));

        log.info("✅ 加载了 {} 个 ASR Provider，默认使用: {} ({})",
                providers.size(),
                defaultProvider.getDisplayName(),
                defaultProvider.getProviderName());

        // 输出所有Provider信息
        providers.forEach(provider ->
                log.info("  - {} ({}) - 优先级: {}, 启用: {}, 支持格式: {}",
                        provider.getDisplayName(),
                        provider.getProviderName(),
                        provider.getPriority(),
                        provider.isEnabled(),
                        String.join(", ", provider.getSupportedFormats()))
        );
    }

    /**
     * 获取默认 Provider
     *
     * @return 默认的 ASR Provider
     */
    public AsrProvider getDefaultProvider() {
        return defaultProvider;
    }

    /**
     * 根据名称获取 Provider
     *
     * @param providerName Provider名称
     * @return 指定的Provider，如果未找到或未启用则返回默认Provider
     */
    public AsrProvider getProvider(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            return defaultProvider;
        }

        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName.trim()))
                .filter(AsrProvider::isEnabled)
                .findFirst()
                .orElseGet(() -> {
                    log.warn("未找到名为 '{}' 的启用的 ASR Provider，使用默认Provider", providerName);
                    return defaultProvider;
                });
    }

    /**
     * 获取所有启用的 Provider
     *
     * @return 所有启用的Provider列表，按优先级排序
     */
    public List<AsrProvider> getAllProviders() {
        return providers.stream()
                .filter(AsrProvider::isEnabled)
                .sorted(Comparator.comparingInt(AsrProvider::getPriority))
                .toList();
    }

    /**
     * 检查是否支持指定格式
     *
     * @param format 音频格式
     * @return 是否支持
     */
    public boolean isSupportedFormat(String format) {
        return defaultProvider.getSupportedFormats() != null &&
                List.of(defaultProvider.getSupportedFormats()).contains(format.toLowerCase());
    }
}
