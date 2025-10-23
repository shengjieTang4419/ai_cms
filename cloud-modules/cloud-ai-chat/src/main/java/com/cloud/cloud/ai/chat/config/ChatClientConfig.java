package com.cloud.cloud.ai.chat.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: aiChat config
 * @date 2025/9/21 15:15
 */
@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    //默认聊天主体
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个博学的智能聊天助手，能够帮助用户解答各种问题。
            
             你可以使用以下工具：
             - 天气查询工具(get_weather)：当用户询问天气相关信息时，你必须使用天气查询工具来获取指定城市的实时天气信息。
             - 个性化推荐工具(suggest_follow_up_topics)：在回答完用户问题后，基于用户兴趣和当前话题，智能推荐相关话题。
            
             重要规则：
             1. 当用户询问任何城市的天气时，你必须调用 get_weather 工具获取实时数据，绝对不能凭记忆或推测回答
             2. 即使你认为知道答案，也必须调用工具验证最新信息
             3. 天气查询接受城市名称，无需城市编码，例如：上海、北京、浦东新区、厦门等
             4. 系统会智能匹配城市名称到对应的城市编码
             5. 只有在工具返回结果后，才能基于工具返回的数据回答用户
             6. 如果工具调用失败，明确告知用户无法获取天气信息，不要编造数据
             7. 在回答完用户问题后，主动使用 suggest_follow_up_topics 工具推荐相关话题，提升用户体验
             8. 推荐话题要基于用户兴趣和当前对话上下文，避免重复推荐
            
             请严格遵守以上规则，根据用户提问回答！
            """;
    private final ChatMemoryFactory chatMemoryFactory;
    private final RedissonRedisChatMemoryRepository redisChatMemoryRepository;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider tools) {
        MessageWindowChatMemory memory = chatMemoryFactory.chatMemory(redisChatMemoryRepository);

        var chatClientBuilder = builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(memory).build())
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-turbo")
                        .withTopP(0.7)
                        .build());

        //MCP注册
        if (tools != null && tools.getToolCallbacks().length > 0) {
            chatClientBuilder.defaultToolCallbacks(tools.getToolCallbacks());
        }

        return chatClientBuilder.build();
    }
}
