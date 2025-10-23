package com.cloud.cloud.ai.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: AI驱动的智能标签提取服务
 * @date 2025/01/16 10:30
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AITagExtractionService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * AI智能提取聊天标签
     */
    public List<String> extractChatTagsWithAI(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String prompt = buildTagExtractionPrompt(content);
            
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();
            
            return parseTagsFromResponse(response);
            
        } catch (Exception e) {
            log.error("AI标签提取失败，使用备用方案", e);
            return extractChatTagsFallback(content);
        }
    }

    /**
     * 异步AI标签提取（用于批量处理）
     */
    @Async
    public void extractChatTagsAsync(String content, Long userId, String sessionId, 
                                   TagExtractionCallback callback) {
        try {
            List<String> tags = extractChatTagsWithAI(content);
            callback.onSuccess(tags);
        } catch (Exception e) {
            log.error("异步AI标签提取失败", e);
            callback.onError(e);
        }
    }

    /**
     * 构建标签提取提示词
     */
    private String buildTagExtractionPrompt(String content) {
        return String.format("""
            请分析以下聊天内容，提取出用户真正感兴趣的话题标签。
            
            要求：
            1. 只提取用户主动讨论、询问或表达兴趣的话题
            2. 忽略负面情感的话题（如抱怨、批评）
            3. 忽略纯信息查询（如"现在几点"、"天气如何"）
            4. 标签要简洁明确，2-4个字
            5. 最多提取5个标签
            6. 返回JSON格式：{"tags": ["标签1", "标签2", "标签3"]}
            
            聊天内容：
            %s
            
            请分析并返回标签：
            """, content);
    }

    /**
     * 解析AI响应中的标签
     */
    private List<String> parseTagsFromResponse(String response) {
        try {
            // 尝试解析JSON格式
            if (response.trim().startsWith("{")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(response, Map.class);
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) result.get("tags");
                return tags != null ? tags : new ArrayList<>();
            }
            
            // 如果不是JSON格式，尝试提取标签
            return extractTagsFromText(response);
            
        } catch (JsonProcessingException e) {
            log.warn("解析AI响应失败，尝试文本提取: {}", response);
            return extractTagsFromText(response);
        }
    }

    /**
     * 从文本中提取标签
     */
    private List<String> extractTagsFromText(String text) {
        List<String> tags = new ArrayList<>();
        
        // 查找可能的标签模式
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("标签") || line.contains("话题")) {
                // 提取引号内的内容
                String[] parts = line.split("[\"']");
                for (int i = 1; i < parts.length; i += 2) {
                    if (parts[i].length() >= 2 && parts[i].length() <= 6) {
                        tags.add(parts[i]);
                    }
                }
            }
        }
        
        return tags.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * 备用标签提取方案（传统关键词匹配）
     */
    private List<String> extractChatTagsFallback(String content) {
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

        List<String> extractedTags = new ArrayList<>();
        String lowerContent = content.toLowerCase();

        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            String tagName = entry.getKey();
            List<String> keywords = entry.getValue();

            boolean found = keywords.stream().anyMatch(keyword -> 
                lowerContent.contains(keyword.toLowerCase()));

            if (found) {
                extractedTags.add(tagName);
            }
        }

        return extractedTags.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * 标签提取回调接口
     */
    public interface TagExtractionCallback {
        void onSuccess(List<String> tags);
        void onError(Exception e);
    }
}
