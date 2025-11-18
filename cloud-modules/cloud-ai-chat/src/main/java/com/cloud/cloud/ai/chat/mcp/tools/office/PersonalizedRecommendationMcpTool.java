package com.cloud.cloud.ai.chat.mcp.tools.office;

import com.cloud.cloud.ai.chat.domain.UserTags;
import com.cloud.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.cloud.ai.chat.mcp.api.Schema;
import com.cloud.cloud.ai.chat.provider.ModelProvider;
import com.cloud.cloud.ai.chat.provider.ModelProviderManager;
import com.cloud.cloud.ai.chat.repository.UserTagsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化推荐工具 - 基于用户画像和聊天行为
 * <p>
 * 注意：这个工具同时提供普通方法供AIChatService直接调用
 *
 * @author shengjie.tang
 * @version 2.0.0
 * @date 2025/11/16
 */
@Component
@Slf4j
public class PersonalizedRecommendationMcpTool implements McpTool {

    private final UserTagsRepository userTagsRepository;

    @Autowired
    @Lazy
    private ModelProviderManager providerManager;

    public PersonalizedRecommendationMcpTool(UserTagsRepository userTagsRepository) {
        this.userTagsRepository = userTagsRepository;
    }

    /**
     * 获取默认ChatClient
     */
    private ChatClient getChatClient() {
        ModelProvider defaultProvider = providerManager.getDefaultProvider();
        return defaultProvider.getChatClient();
    }

    @Override
    public String getName() {
        return "suggest_follow_up_topics";
    }

    @Override
    public String getDescription() {
        return "基于当前对话和用户兴趣，智能推荐相关话题。" +
                "根据用户的兴趣标签和当前对话主题，生成3个相关的后续话题建议。";
    }

    @Override
    public Schema getInputSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("query", Schema.string("当前对话主题"));
        properties.put("userId", Schema.integer("用户ID"));
        return Schema.object(properties, Arrays.asList("query", "userId"));
    }

    @Override
    public Schema getOutputSchema() {
        return Schema.array(
                Schema.string("推荐的话题"),
                "推荐话题列表，最多3个"
        );
    }

    @Override
    public Object execute(JsonNode input) throws Exception {
        String query = input.get("query").asText();
        Long userId = input.get("userId").asLong();

        return suggestFollowUpTopics(query, userId);
    }

    @Override
    public String getCategory() {
        return "office";
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    /**
     * 推荐后续话题
     * <p>
     * 这个方法同时被：
     * 1. MCP工具系统调用（通过execute方法）
     * 2. AIChatService直接调用（异步生成推荐）
     *
     * @param currentTopic 当前对话主题
     * @param userId       用户ID
     * @return 推荐话题列表
     */
    public List<String> suggestFollowUpTopics(String currentTopic, Long userId) {
        log.info("推荐相关话题，userId={}, currentTopic={}", userId, currentTopic);

        try {
            // 获取用户热门标签
            List<UserTags> userTags = userTagsRepository.findTop5ByUserIdOrderByTotalWeightDesc(userId);

            if (userTags.isEmpty()) {
                return new ArrayList<>();
            }

            // 使用AI找到与当前话题相关的标签
            List<UserTags> relatedTags = findRelatedTagsWithAI(userTags, currentTopic);

            if (relatedTags.isEmpty()) {
                // 如果没有直接相关的，推荐用户最感兴趣的话题
                relatedTags = userTags.stream().limit(3).toList();
            }

            List<String> suggestTopics = new ArrayList<>();
            for (UserTags tag : relatedTags) {
                String suggestion = generateFollowUpSuggestionWithAI(tag.getTagName(), currentTopic);
                suggestTopics.add(suggestion);
            }

            return suggestTopics;
        } catch (Exception e) {
            log.error("推荐相关话题失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 使用AI找到与当前话题相关的标签
     */
    private List<UserTags> findRelatedTagsWithAI(List<UserTags> userTags, String currentTopic) {
        try {
            String tagList = userTags.stream()
                    .map(UserTags::getTagName)
                    .collect(Collectors.joining("、"));

            String prompt = String.format("""
                    请从以下用户兴趣标签中，找出与当前话题"%s"最相关的标签（最多3个）：
                    
                    用户兴趣标签：%s
                    当前话题：%s
                    
                    要求：
                    1. 基于语义相关性判断，不仅仅是关键词匹配
                    2. 考虑标签与话题的关联程度
                    3. 返回格式：标签1,标签2,标签3
                    4. 如果没有相关标签，返回空字符串
                    
                    相关标签：
                    """, currentTopic, tagList, currentTopic);

            String response = getChatClient().prompt(prompt).call().content();

            if (response == null || response.trim().isEmpty()) {
                return new ArrayList<>();
            }

            // 解析AI返回的标签
            String[] relatedTagNames = response.trim().split(",");
            List<String> relatedTagNameList = Arrays.stream(relatedTagNames)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .toList();

            // 找到对应的UserTags对象
            return userTags.stream()
                    .filter(tag -> relatedTagNameList.contains(tag.getTagName()))
                    .limit(3)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("AI查找相关标签失败，使用备用方案", e);
            // 备用方案：返回权重最高的前3个标签
            return userTags.stream().limit(3).collect(Collectors.toList());
        }
    }

    /**
     * 使用AI生成后续建议
     */
    private String generateFollowUpSuggestionWithAI(String tagName, String currentTopic) {
        try {
            String prompt = String.format("""
                    基于用户当前话题"%s"和兴趣标签"%s"，生成一个自然的后续建议问题。
                    
                    要求：
                    1. 问题要自然，像朋友间的对话
                    2. 要体现标签与当前话题的关联
                    3. 问题长度控制在20-40个字
                    4. 直接返回问题，不要额外解释
                    
                    当前话题：%s
                    兴趣标签：%s
                    生成的建议：
                    """, currentTopic, tagName, currentTopic, tagName);

            String response = getChatClient().prompt(prompt).call().content();
            return response != null ? response.trim() : "您是否想了解更多关于 " + tagName + " 的知识？";

        } catch (Exception e) {
            log.error("AI生成后续建议失败，使用默认建议", e);
            return "您是否想了解更多关于 " + tagName + " 的知识？";
        }
    }
}
