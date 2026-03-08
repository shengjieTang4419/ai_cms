package com.cloud.ai.chat.service.impl;

import com.cloud.ai.chat.provider.ModelProvider;
import com.cloud.ai.chat.provider.ModelProviderManager;
import com.cloud.ai.chat.util.PromptLoader;
import com.cloud.memebership.domain.TagPath;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: AI驱动的智能标签提取服务 - 使用SPI获取模型
 * @date 2025/01/16 10:30
 */
@Service
@Slf4j
public class AITagExtractionService {

    private final ModelProviderManager providerManager;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    public AITagExtractionService(ModelProviderManager providerManager,
                                  ObjectMapper objectMapper,
                                  PromptLoader promptLoader) {
        this.providerManager = providerManager;
        this.objectMapper = objectMapper;
        this.promptLoader = promptLoader;
    }

    /**
     * 获取默认ChatClient
     */
    private ChatClient getChatClient() {
        ModelProvider defaultProvider = providerManager.getDefaultProvider();
        return defaultProvider.getChatClient();
    }

    /**
     * AI智能提取聊天标签
     */
    public TagPath extractChatTagsWithAI(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        try {
            String prompt = buildTagExtractionPrompt(content);
            String response = getChatClient().prompt(prompt)
                    .call()
                    .content();

            return parseTagPathFromResponse(Objects.requireNonNull(response));

        } catch (Exception e) {
            log.error("AI标签提取失败，使用备用方案", e);
            return extractChatTagsFallback(content);
        }
    }

    /**
     * 构建标签提取提示词
     */
    private String buildTagExtractionPrompt(String content) {
        return promptLoader.loadPromptAndFormat("prompts/tag-extraction-prompt.txt", content);
    }

    /**
     * 解析AI响应中的路径
     */
    private TagPath parseTagPathFromResponse(String response) {
        try {
            // 尝试解析JSON格式
            String trimmed = response.trim();
            if (trimmed.startsWith("{")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(trimmed, Map.class);

                Object pathObj = result.get("path");
                if (pathObj instanceof List<?> list) {
                    List<String> path = coerceStringList(list);
                    return buildTagPath(path);
                }

                return null;
            }

            // 如果不是JSON格式，尝试从文本中提取一条路径
            return extractPathFromText(trimmed);

        } catch (JsonProcessingException e) {
            log.warn("解析AI响应失败，尝试文本提取: {}", response);
            return extractPathFromText(response);
        }
    }

    /**
     * 从文本中提取路径（兜底）
     */
    private TagPath extractPathFromText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // 尝试解析形如：人工智能/多模态/视觉理解
        String candidate = trimmed;
        int newlineIdx = candidate.indexOf('\n');
        if (newlineIdx > 0) {
            candidate = candidate.substring(0, newlineIdx);
        }
        candidate = candidate.replace("\\u3000", " ").trim();
        candidate = candidate.replace("->", "/").replace(">", "/").replace(" ", "");

        if (candidate.contains("/")) {
            List<String> path = Arrays.stream(candidate.split("/"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            return buildTagPath(path);
        }

        // 否则当成单级类目
        return buildTagPath(Collections.singletonList(candidate));
    }

    /**
     * 备用标签提取方案（传统关键词匹配）
     */
    private TagPath extractChatTagsFallback(String content) {
        // 简化的关键词映射
        Map<String, List<String>> keywordMap = Map.of(
                "编程", Arrays.asList("编程", "代码", "开发", "技术", "软件", "程序", "算法", "bug", "调试"),
                "天气", Arrays.asList("天气", "温度", "下雨", "晴天", "阴天", "气温", "气候", "预报"),
                "旅游", Arrays.asList("旅游", "旅行", "景点", "风景", "度假", "出行", "游玩", "攻略"),
                "美食", Arrays.asList("美食", "做饭", "餐厅", "料理", "烹饪", "吃", "食物", "菜谱"),
                "运动", Arrays.asList("运动", "健身", "跑步", "游泳", "锻炼", "体育", "健身", "训练"),
                "摄影", Arrays.asList("摄影", "拍照", "相机", "照片", "拍摄", "影像", "镜头", "构图"),
                "音乐", Arrays.asList("音乐", "歌曲", "歌手", "演唱会", "乐器", "旋律", "节奏", "专辑"),
                "电影", Arrays.asList("电影", "影片", "导演", "演员", "影院", "票房", "剧情", "影评"),
                "读书", Arrays.asList("读书", "书籍", "小说", "文学", "阅读", "作者", "出版", "书评"),
                "游戏", Arrays.asList("游戏", "电竞", "玩家", "攻略", "装备", "副本", "竞技", "steam")
        );

        List<TagPath> extractedTagPaths = new ArrayList<>();
        String lowerContent = content.toLowerCase();

        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            String tagName = entry.getKey();
            List<String> keywords = entry.getValue();

            boolean found = keywords.stream().anyMatch(keyword ->
                    lowerContent.contains(keyword.toLowerCase()));

            if (found) {
                TagPath tagPath = buildTagPath(Collections.singletonList(tagName));
                if (tagPath != null) {
                    extractedTagPaths.add(tagPath);
                }
            }
        }

        return extractedTagPaths.stream().findFirst().orElse(null);
    }

    private List<String> coerceStringList(List<?> list) {
        List<String> result = new ArrayList<>();
        for (Object o : list) {
            if (o == null) {
                continue;
            }
            String s = String.valueOf(o).trim();
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result;
    }

    private TagPath buildTagPath(List<String> rawPath) {
        if (rawPath == null) {
            return null;
        }
        List<String> path = rawPath.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (path.isEmpty()) {
            return null;
        }

        if (path.size() < 3) {
            log.warn("AI返回的话题层级不足3层，忽略本次标签沉淀，path={}", path);
            return null;
        }

        TagPath tagPath = new TagPath();
        tagPath.setPath(path);
        tagPath.setKey(String.join("/", path));
        return tagPath;
    }

    /**
     * 标签提取回调接口
     */
    public interface TagExtractionCallback {
        void onSuccess(TagPath tagPath);

        void onError(Exception e);
    }
}
