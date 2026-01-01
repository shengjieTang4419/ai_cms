package com.cloud.ai.chat.provider;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 模型提供者管理器 - SPI机制核心
 * <p>
 * 自动发现并管理所有ModelProvider实现
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: SPI服务管理器
 * @date 2025/10/14
 */
@Component
@Slf4j
public class ModelProviderManager {

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, ModelProvider> providers = new HashMap<>();
    /**
     * -- GETTER --
     * 获取默认Provider
     */
    @Getter
    private ModelProvider defaultProvider;

    @PostConstruct
    public void init() {
        log.info("🚀 开始加载ModelProvider实现...");

        try {
            // 从Spring容器中获取所有ModelProvider实现
            // 使用getBeanNamesForType先获取名称，避免在初始化阶段触发bean创建
            String[] beanNames = applicationContext.getBeanNamesForType(ModelProvider.class, false, false);
            log.info("发现 {} 个ModelProvider实现类", beanNames.length);

            // 逐个获取bean实例
            List<ModelProvider> allProviders = new ArrayList<>();
            for (String beanName : beanNames) {
                try {
                    ModelProvider provider = applicationContext.getBean(beanName, ModelProvider.class);
                    allProviders.add(provider);
                    log.debug("成功加载Provider: {}", beanName);
                } catch (Exception e) {
                    log.warn("加载Provider失败: {}，错误: {}", beanName, e.getMessage());
                }
            }

            log.info("成功加载 {} 个ModelProvider实现", allProviders.size());

            // 过滤并排序
            List<ModelProvider> enabledProviders = allProviders.stream()
                    .filter(ModelProvider::isEnabled)
                    .sorted(Comparator.comparingInt(ModelProvider::getPriority))
                    .toList();

            if (enabledProviders.isEmpty()) {
                log.error("❌ 没有可用的ModelProvider实现！请检查配置。");
                throw new IllegalStateException("没有可用的ModelProvider实现，系统无法启动");
            }

            // 注册所有Provider
            for (ModelProvider provider : enabledProviders) {
                providers.put(provider.getModelName(), provider);
                log.info("✅ 注册模型: {} - {} (优先级: {})",
                        provider.getModelName(),
                        provider.getDisplayName(),
                        provider.getPriority());
            }

            // 选择默认Provider：优先选择非Vision模型，如果没有则选择优先级最高的
            defaultProvider = enabledProviders.stream()
                    .filter(p -> !p.supportsVision())
                    .findFirst()
                    .orElse(enabledProviders.get(0));

            log.info("📊 Provider优先级排序: {}",
                    enabledProviders.stream()
                            .map(p -> p.getModelName() + "(" + p.getPriority() + ")")
                            .collect(java.util.stream.Collectors.joining(" -> ")));

            log.info("🎯 默认模型: {} - {}",
                    defaultProvider.getModelName(),
                    defaultProvider.getDisplayName());
        } catch (Exception e) {
            log.error("❌ ModelProvider初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("ModelProvider初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据模型名称获取Provider
     */
    public ModelProvider getProvider(String modelName) {
        ModelProvider provider = providers.get(modelName);
        if (provider == null) {
            log.warn("模型 {} 不存在，使用默认模型", modelName);
            return defaultProvider;
        }
        return provider;
    }

    /**
     * 根据是否需要Vision功能获取合适的Provider
     */
    public ModelProvider getProviderByCapability(boolean needsVision) {
        if (needsVision) {
            return providers.values().stream()
                    .filter(ModelProvider::supportsVision)
                    .min(Comparator.comparingInt(ModelProvider::getPriority))
                    .orElse(defaultProvider);
        }
        return defaultProvider;
    }

    /**
     * 获取优先级最高的支持Thinking的Provider
     * 
     * @return 支持Thinking的Provider，如果没有则返回默认Provider
     */
    public ModelProvider getThinkingProvider() {
        return providers.values().stream()
                .filter(ModelProvider::supportsThinking)
                .min(Comparator.comparingInt(ModelProvider::getPriority))
                .orElseGet(() -> {
                    log.warn("没有找到支持Thinking的模型，使用默认模型");
                    return defaultProvider;
                });
    }

    /**
     * 获取所有Provider
     */
    public Collection<ModelProvider> getAllProviders() {
        return providers.values();
    }

    /**
     * 获取所有支持的模型名称
     */
    public Set<String> getAvailableModels() {
        return providers.keySet();
    }

    /**
     * 检查模型是否存在
     */
    public boolean exists(String modelName) {
        return providers.containsKey(modelName);
    }
}

