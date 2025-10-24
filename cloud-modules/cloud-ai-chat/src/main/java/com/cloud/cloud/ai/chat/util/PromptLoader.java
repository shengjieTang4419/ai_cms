package com.cloud.cloud.ai.chat.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 提示词加载工具类
 * @date 2025/10/23 19:00
 */
@Component
@Slf4j
public class PromptLoader {

    /**
     * 从资源文件加载提示词
     *
     * @param resourcePath 资源文件路径，如：prompts/system-prompt.txt
     * @return 提示词内容
     */
    public String loadPrompt(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.error("提示词文件不存在: {}", resourcePath);
                return "";
            }

            try (InputStreamReader reader = new InputStreamReader(
                    resource.getInputStream(), StandardCharsets.UTF_8)) {
                String content = FileCopyUtils.copyToString(reader);
                log.info("成功加载提示词文件: {}, 长度: {} 字符", resourcePath, content.length());
                return content;
            }
        } catch (IOException e) {
            log.error("加载提示词文件失败: {}", resourcePath, e);
            return "";
        }
    }

    /**
     * 加载默认系统提示词
     *
     * @return 系统提示词内容
     */
    public String loadSystemPrompt() {
        return loadPrompt("prompts/system-prompt.txt");
    }
}

