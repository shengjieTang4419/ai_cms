package com.cloud.ai.chat.service.impl;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cloud.ai.chat.domain.ChatContext;
import com.cloud.ai.chat.domain.Image;
import com.cloud.ai.chat.helper.ChatAnalysisHelper;
import com.cloud.ai.chat.mcp.tools.office.PersonalizedRecommendationMcpTool;
import com.cloud.ai.chat.provider.ModelProvider;
import com.cloud.ai.chat.util.ModelSelector;
import com.cloud.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: AiService - 支持多模型切换（SPI版本）
 * @date 2025/9/21 15:16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatService {

    private final ModelSelector modelSelector;
    private final ChatTitleService chatTitleService;
    private final ChatDialogueService chatDialogueService;
    private final ChatMessageService chatMessageService;
    private final PgVectorStore vectorStore;
    private final ChatAnalysisHelper chatAnalysisHelper;
    private final PersonalizedRecommendationMcpTool recommendationTool;
    private final ImageService imageService;

    @Value("${ai.guide:true}")
    private boolean aiGuide;


    /**
     * 根据聊天上下文获取ChatClient实例
     * 基于完整的请求上下文（包括query、session、images等）智能选择最合适的模型
     *
     * @param context 聊天上下文（包含useThinking标识）
     */
    private ChatClient getChatClient(ChatContext context) {
        // 根据上下文智能选择模型（包括Thinking模型的选择）
        ModelProvider provider = modelSelector.selectModelProvider(context);
        log.info("选择模型: {} ({}) - 上下文: images={}, rag={}, webSearch={}, useThinking={}",
                provider.getDisplayName(), provider.getModelName(),
                context.getImageCount(), context.isRagEnhanced(),
                context.isWithEnableSearch(), context.isUseThinking());
        return provider.getChatClient();
    }

    public String simpleChat(String query) {
        ModelProvider defaultProvider = modelSelector.getProvider("qwen-turbo");
        return defaultProvider.getChatClient().prompt(query).call().content();
    }

    public Flux<String> streamChat(String query, String sessionId, List<String> imageUrlList, Boolean isWithEnableSearch, Boolean isDeepThinking, String longitude, String latitude) {
        return streamChat(query, sessionId, false, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList, isDeepThinking, longitude, latitude);
    }

    public Flux<String> ragStreamChat(String query, String sessionId, List<String> imageUrlList, Boolean isWithEnableSearch, Boolean isDeepThinking, String longitude, String latitude) {
        boolean useThinking = Boolean.TRUE.equals(isDeepThinking);
        List<Document> relevantDocs = vectorStore.similaritySearch(query);
        if (CollectionUtils.isEmpty(relevantDocs)) {
            return streamChat(query, sessionId, false, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList, useThinking, longitude, latitude);
        }
        String enhancedPrompt = buildRagPrompt(query, relevantDocs);
        return streamChat(enhancedPrompt, sessionId, true, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList, useThinking, longitude, latitude);
    }

    /**
     * 流式聊天核心方法
     *
     * @param query         增强后的查询（可能包含OCR文本）
     * @param sessionId     会话ID
     * @param isRagEnhanced 是否启用RAG增强
     * @param isWebSearch   是否启用全网搜索
     * @param originQuery   原始用户查询
     * @param imageList     图片URL列表
     * @param useThinking   是否使用Thinking模型（深度思考）
     * @param longitude     当前位置经度（可选）
     * @param latitude      当前位置纬度（可选）
     * @return 响应流
     */
    private Flux<String> streamChat(String query, String sessionId, boolean isRagEnhanced, boolean isWebSearch, String originQuery, List<String> imageList, boolean useThinking, String longitude, String latitude) {
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("开始流式对话，userId: {}, sessionId: {}, query: {}, images: {}, rag={}, webSearch={}, deepThinking={}",
                userId, sessionId, originQuery, imageList != null ? imageList.size() : 0, isRagEnhanced, isWebSearch, useThinking);

        // 1. 处理会话初始化（在流开始前完成）
        initializeSessionIfNeeded(userId, sessionId, originQuery);

        // 2. 异步生成个性化推荐（如果启用）
        CompletableFuture<List<String>> recommendationsFuture = startRecommendationGeneration(userId, sessionId, originQuery);

        // 3. 使用OCR结果增强查询
        String enhancedQuery = enhanceQueryWithOCR(query, imageList);

        // 4. 构建聊天上下文并获取ChatClient
        ChatContext context = buildChatContext(enhancedQuery, sessionId, isRagEnhanced, isWebSearch, originQuery, imageList, userId, useThinking, longitude, latitude);
        ChatClient chatClient = getChatClient(context);

        // 5. 构建并执行请求流（如果有location信息，将其添加到prompt中）
        String finalQuery = enhancedQuery;
        if (context.hasLocation()) {
            // 如果有位置信息，将其添加到查询中供MCP工具使用
            finalQuery = enhancedQuery + "\n\n[我的当前位置坐标：" + context.getLocationCoordinate() + "]";
            log.info("检测到位置信息，已添加到查询中: {}", context.getLocationCoordinate());
        }
        Flux<String> contentFlux = buildRequestStream(chatClient, finalQuery, sessionId, isWebSearch, useThinking);

        // 6. 处理响应流：收集内容、追加推荐、保存消息
        return processResponseStream(contentFlux, recommendationsFuture, userId, sessionId, originQuery, imageList, isRagEnhanced);
    }

    /**
     * 初始化会话（如果是新会话则创建标题）
     */
    private void initializeSessionIfNeeded(Long userId, String sessionId, String originQuery) {
        if (chatDialogueService.isNewSession(sessionId)) {
            try {
                chatTitleService.createSessionWithTitle(userId, sessionId, originQuery);
                log.debug("新会话已初始化，sessionId: {}", sessionId);
            } catch (Exception e) {
                log.error("初始化会话失败，sessionId: {}", sessionId, e);
            }
        }
    }

    /**
     * 启动个性化推荐生成（异步）
     */
    private CompletableFuture<List<String>> startRecommendationGeneration(Long userId, String sessionId, String originQuery) {
        if (!aiGuide) {
            return null;
        }

        log.debug("异步生成个性化推荐，userId: {}, sessionId: {}, query: {}", userId, sessionId, originQuery);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return recommendationTool.suggestFollowUpTopics(originQuery, userId);
            } catch (Exception e) {
                log.error("生成个性化推荐失败，userId: {}, sessionId: {}", userId, sessionId, e);
                return null;
            }
        });
    }

    /**
     * 构建聊天上下文
     */
    private ChatContext buildChatContext(String enhancedQuery, String sessionId, boolean isRagEnhanced,
                                         boolean isWebSearch, String originQuery, List<String> imageList, Long userId, boolean useThinking, String longitude, String latitude) {
        return ChatContext.builder().query(enhancedQuery).sessionId(sessionId)
                .ragEnhanced(isRagEnhanced).withEnableSearch(isWebSearch).originQuery(originQuery)
                .imageUrls(imageList).userId(userId).useThinking(useThinking)
                .longitude(longitude).latitude(latitude).build();
    }

    /**
     * 构建请求流
     *
     * @param chatClient    ChatClient实例
     * @param enhancedQuery 增强后的查询
     * @param sessionId     会话ID
     * @param isWebSearch   是否启用全网搜索
     * @param isThinking    是否是Thinking模型（需要输出推理过程）
     */
    private Flux<String> buildRequestStream(ChatClient chatClient, String enhancedQuery, String sessionId, boolean isWebSearch, boolean isThinking) {
        var promptBuilder = chatClient.prompt(enhancedQuery)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId));

        // 构建请求选项
        var optionsBuilder = DashScopeChatOptions.builder();

        // 当启用全网搜索时，设置enableSearch选项
        if (isWebSearch) {
            optionsBuilder.withEnableSearch(true);
            log.info("为本次请求启用DashScope全网搜索功能，sessionId: {}", sessionId);
        }

        // Thinking模型必须启用enable_thinking参数
        if (isThinking) {
            optionsBuilder.withEnableThinking(true);
            log.info("为本次请求启用DashScope思考模型功能，sessionId: {}", sessionId);
        }

        DashScopeChatOptions requestOptions = optionsBuilder.build();
        promptBuilder = promptBuilder.options(requestOptions);

        // 如果启用思考模型，需要处理reasoning content和content两部分
        // Spring AI的ChatClient可能不直接暴露reasoning content，我们通过流式响应处理
        if (isThinking) {
            return buildThinkingStream(promptBuilder);
        } else {
            return promptBuilder.stream().content();
        }
    }

    /**
     * 构建思考模型的流式响应（包含思考过程和最终回复）
     * 通过处理流式响应，尝试提取reasoning content和content
     * <p>
     * 注意：由于Spring AI的ChatClient可能不直接暴露reasoning content，
     * 我们暂时只返回content。如果需要完整的思考过程，建议直接使用DashScope的底层API。
     * 参考官方示例：https://bailian.console.aliyun.com/?tab=model#/model-market/detail/qwen3-next-80b-a3b-thinking
     */
    private Flux<String> buildThinkingStream(ChatClient.ChatClientRequestSpec promptBuilder) {
        // 目前Spring AI的ChatClient可能不直接支持reasoning content的获取
        // 我们暂时返回普通content，后续可以通过以下方式支持：
        // 1. 升级Spring AI版本（如果后续版本支持）
        // 2. 直接使用DashScope的底层API（参考官方示例代码）
        // 3. 通过反射从底层响应中提取reasoning content（需要深入了解Spring AI的实现）

        log.info("思考模型已启用，但当前实现可能无法获取完整的思考过程");
        log.info("建议：如果需要完整的思考过程，请参考官方示例直接使用DashScope的Generation API");

        // 暂时返回普通content流
        return promptBuilder.stream().content();
    }

    /**
     * 处理响应流：收集内容、追加推荐、保存消息
     */
    private Flux<String> processResponseStream(Flux<String> contentFlux, CompletableFuture<List<String>> recommendationsFuture,
                                               Long userId, String sessionId, String originQuery,
                                               List<String> imageList, boolean isRagEnhanced) {
        StringBuilder fullResponse = new StringBuilder();

        // 主响应流：收集AI响应内容
        Flux<String> mainResponseFlux = contentFlux
                .doOnNext(fullResponse::append)
                .doOnError(error -> log.error("流式响应处理出错，userId: {}, sessionId: {}", userId, sessionId, error));

        // 在主响应流完成后，追加推荐内容
        // concatWith 会在主响应流完成后才订阅推荐流，Flux.defer 确保在订阅时才获取推荐内容
        Flux<String> recommendationsFlux = Flux.defer(() -> {
            try {
                String recommendations = getRecommendationsSafely(recommendationsFuture);
                if (StringUtils.hasText(recommendations)) {
                    log.debug("个性化推荐已获取并追加，sessionId: {}", sessionId);
                    return Flux.just(recommendations);
                }
            } catch (Exception e) {
                log.warn("获取推荐内容失败，sessionId: {}", sessionId, e);
            }
            return Flux.empty();
        });

        // 合并主响应流和推荐流（concatWith 确保主响应流完成后才订阅推荐流）
        return mainResponseFlux
                .concatWith(recommendationsFlux)
                .doOnComplete(() -> {
                    String completeResponse = fullResponse.toString();
                    // 再次获取推荐内容以确保保存时包含推荐（防止异步获取延迟）
                    String recommendations = getRecommendationsSafely(recommendationsFuture);
                    if (StringUtils.hasText(recommendations)) {
                        completeResponse += recommendations;
                    }

                    // 保存消息
                    saveMessages(userId, sessionId, originQuery, imageList, completeResponse, isRagEnhanced);

                    // 异步分析聊天内容并更新用户标签
                    if (aiGuide) {
                        analyzeChatContentAsync(userId, sessionId, originQuery, completeResponse);
                    }
                });
    }

    /**
     * 安全获取推荐内容（带超时）
     */
    private String getRecommendationsSafely(CompletableFuture<List<String>> recommendationsFuture) {
        if (recommendationsFuture == null || !aiGuide) {
            return "";
        }

        try {
            List<String> recommendationList = recommendationsFuture.get(500, TimeUnit.MILLISECONDS);
            if (!CollectionUtils.isEmpty(recommendationList)) {
                return formatRecommendations(recommendationList);
            }
        } catch (Exception e) {
            log.debug("获取个性化推荐超时或失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 保存用户消息和助手消息
     */
    private void saveMessages(Long userId, String sessionId, String originQuery,
                              List<String> imageList, String completeResponse, boolean isRagEnhanced) {
        try {
            chatMessageService.saveUserMessage(sessionId, userId, originQuery, imageList);
            chatMessageService.saveAssistantMessage(sessionId, userId, completeResponse, isRagEnhanced);
            log.debug("消息已保存，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("保存消息失败，sessionId: {}", sessionId, e);
        }
    }

    /**
     * 使用OCR结果增强查询
     * 有图片就解析OCR文字并附加到query，没有图片直接返回原query
     */
    private String enhanceQueryWithOCR(String query, List<String> imageList) {
        // 没有图片，直接返回原始查询
        if (imageList == null || imageList.isEmpty()) {
            return query;
        }

        log.info("检测到{}张图片，提取OCR文字增强查询", imageList.size());
        StringBuilder enhancedQuery = new StringBuilder(query);

        for (String imageUrl : imageList) {
            Image image = imageService.findByUrl(imageUrl);
            if (image != null && StringUtils.hasText(image.getOcrText())) {
                enhancedQuery.append("\n\n从图片中提取的文字：\n").append(image.getOcrText());
                log.debug("成功提取图片OCR文字: {}", imageUrl);
            } else {
                log.warn("图片OCR未完成或文字为空: {}", imageUrl);
            }
        }

        return enhancedQuery.toString();
    }

    /**
     * 格式化推荐内容，使其更加自然和友好
     */
    private String formatRecommendations(List<String> recommendations) {
        if (CollectionUtils.isEmpty(recommendations)) {
            return "";
        }

        // 取前3个推荐（如果有的话）
        int count = Math.min(recommendations.size(), 3);
        List<String> topRecommendations = recommendations.subList(0, count);

        StringBuilder result = new StringBuilder("\n\n要不我们聊点其他的？");

        for (int i = 0; i < topRecommendations.size(); i++) {
            String item = topRecommendations.get(i).trim();

            if (i == 0) {
                // 第一个：比如...
                result.append("比如").append(item).append("？");
            } else if (i == topRecommendations.size() - 1) {
                // 最后一个：或者...
                result.append("或者").append(item).append("？");
            } else {
                // 中间的：、...
                result.append("、").append(item);
            }
        }

        return result.toString();
    }


    private String buildRagPrompt(String userQuery, List<Document> docs) {
        if (docs.isEmpty()) {
            return userQuery; // 没有相关文档时使用原始查询
        }
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        return String.format("""
                基于以下参考文档回答用户问题。如果文档中没有相关信息，请基于你的知识回答，并说明信息来源。
                
                参考文档：
                %s
                
                用户问题：%s
                
                请提供准确、有用的回答：
                """, context, userQuery);
    }


    /**
     * 异步分析聊天内容并更新用户标签
     */
    @Async
    public void analyzeChatContentAsync(Long userId, String sessionId, String userQuery, String assistantResponse) {
        try {
            // 合并用户问题和AI回答作为分析内容
            String combinedContent = String.format("用户问题：%s\nAI回答：%s", userQuery, assistantResponse);

            // 分析聊天内容并更新标签
            chatAnalysisHelper.analyzeChatSession(userId, sessionId, combinedContent);

            log.info("聊天内容分析完成，userId: {}, sessionId: {}", userId, sessionId);
        } catch (Exception e) {
            log.error("聊天内容分析失败，userId: {}, sessionId: {}", userId, sessionId, e);
        }
    }
}
