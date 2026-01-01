package com.cloud.ai.chat.mcp.tools.office;


import com.cloud.ai.chat.mcp.api.McpTool;
import com.cloud.ai.chat.mcp.api.Schema;
import com.cloud.ai.chat.provider.ModelProvider;
import com.cloud.ai.chat.provider.ModelProviderManager;
import com.cloud.common.core.util.StringUtils;
import com.cloud.memebership.api.RemoteUserTagFeignService;
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

    //private final UserTagsRepository userTagsRepository;
    private final RemoteUserTagFeignService remoteUserTagFeignService;

    @Autowired
    @Lazy
    private ModelProviderManager providerManager;


    public PersonalizedRecommendationMcpTool(RemoteUserTagFeignService remoteUserTagFeignService) {
        //this.userTagsRepository = userTagsRepository;
        this.remoteUserTagFeignService = remoteUserTagFeignService;
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
        return suggestFollowUpTopics(query);
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
     * @return 推荐话题列表
     */
    public List<String> suggestFollowUpTopics(String currentTopic) {
        log.info("推荐相关话题，currentTopic={}", currentTopic);

        try {
            String suggestion = generateFollowUpSuggestionWithAI(currentTopic);
            if (StringUtils.isEmpty(suggestion)) {
                return new ArrayList<>();
            }
            
            // 使用|||作为分隔符，更安全
            return Arrays.stream(suggestion.split("\\|\\|\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("推荐相关话题失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 使用AI生成后续建议
     */
    private String generateFollowUpSuggestionWithAI(String currentTopic) {
        try {
            String prompt = String.format("""
                    基于用户当前话题"%s"，生成三个自然的后续建议问题。
                    
                    要求：
                    1. 问题要自然，像朋友间的对话
                    2. 要体现与当前话题的关联
                    3. 问题长度控制在20-40个字
                    4. 直接返回问题，不要额外解释
                    5. 话题与话题之间必须用|||做分割（三个竖线）
                    6. 话题内容中不能包含|||符号
                    7. 返回格式示例：问题1|||问题2|||问题3
                    
                    当前话题：%s
                    生成的建议：
                    """, currentTopic, currentTopic);

            String response = getChatClient().prompt(prompt).call().content();
            assert response != null;
            return response.trim();

        } catch (Exception e) {
            log.error("AI生成后续建议失败，使用默认建议", e);
            return null;
        }
    }
}
