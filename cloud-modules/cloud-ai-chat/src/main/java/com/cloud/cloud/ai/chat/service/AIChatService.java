package com.cloud.cloud.ai.chat.service;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cloud.cloud.ai.chat.domain.Image;
import com.cloud.cloud.ai.chat.dto.ChatContext;
import com.cloud.cloud.ai.chat.helper.ChatAnalysisHelper;
import com.cloud.cloud.ai.chat.mcp.service.tool.PersonalizedRecommendationTools;
import com.cloud.cloud.ai.chat.provider.ModelProvider;
import com.cloud.cloud.ai.chat.util.ModelSelector;
import com.cloud.cloud.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
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
    private final PersonalizedRecommendationTools recommendationTools;
    private final ImageService imageService;


    /**
     * 根据聊天上下文获取ChatClient实例
     * 基于完整的请求上下文（包括query、session、images等）智能选择最合适的模型
     */
    private ChatClient getChatClient(ChatContext context) {
        ModelProvider provider = modelSelector.selectModelProvider(context);
        ChatClient client = provider.getChatClient();
        log.info("选择模型: {} ({}) - 上下文: images={}, rag={}, webSearch={}",
                provider.getDisplayName(),
                provider.getModelName(),
                context.getImageCount(),
                context.isRagEnhanced(),
                context.isWithEnableSearch());
        return client;
    }

    public String simpleChat(String query) {
        ModelProvider defaultProvider = modelSelector.getProvider("qwen-turbo");
        return defaultProvider.getChatClient().prompt(query).call().content();
    }

    public Flux<String> streamChat(String query, String sessionId, List<String> imageUrlList, Boolean isWithEnableSearch) {
        return streamChat(query, sessionId, false, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList);
    }

    public Flux<String> ragStreamChat(String query, String sessionId, List<String> imageUrlList, Boolean isWithEnableSearch) {
        List<Document> relevantDocs = vectorStore.similaritySearch(query);
        if (CollectionUtils.isEmpty(relevantDocs)) {
            return streamChat(query, sessionId, false, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList);
        }
        String enhancedPrompt = buildRagPrompt(query, relevantDocs);
        return streamChat(enhancedPrompt, sessionId, true, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList);
    }

    private Flux<String> streamChat(String query, String sessionId, boolean isRagEnhanced, boolean isWebSearch, String originQuery, List<String> imageList) {
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("开始流式对话，userId: {}, sessionId: {}, query: {}, images: {}, rag={}, webSearch={}",
                userId, sessionId, originQuery, imageList != null ? imageList.size() : 0, isRagEnhanced, isWebSearch);
        
        log.info("异步生成个性化推荐，userId: {}, sessionId: {}, query: {}", userId, sessionId, originQuery);
        CompletableFuture<List<String>> recommendationsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return recommendationTools.suggestFollowUpTopics(originQuery, userId);
            } catch (Exception e) {
                log.error("生成个性化推荐失败，userId: {}, sessionId: {}", userId, sessionId, e);
                return null;
            }
        });

        // 1. 提前处理图片OCR：有图片就解析，没有图片跳过
        String enhancedQuery = enhanceQueryWithOCR(query, imageList);

        // 2. 构建聊天上下文，用于模型选择
        ChatContext context = ChatContext.builder().query(enhancedQuery).sessionId(sessionId)
                .ragEnhanced(isRagEnhanced).withEnableSearch(isWebSearch).originQuery(originQuery).imageUrls(imageList)
                .userId(userId).build();

        // 3. 根据上下文获取合适的ChatClient并执行对话
        // ChatClient是线程安全的，每次prompt()调用都会创建新的请求链，互不影响
        ChatClient chatClient = getChatClient(context);
        
        // 构建请求链：每次调用prompt()都会创建独立的请求上下文，options只影响本次请求
        var promptBuilder = chatClient.prompt(enhancedQuery)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId));
        
        // 当启用全网搜索时，为本次请求设置DashScope的enableSearch选项
        // 注意：这是请求级别的选项，不会影响其他请求，因此线程安全
        if (isWebSearch) {
            DashScopeChatOptions requestOptions = DashScopeChatOptions.builder()
                    .withEnableSearch(true)
                    .build();
            promptBuilder = promptBuilder.options(requestOptions);
            log.info("为本次请求启用DashScope全网搜索功能，sessionId: {}, userId: {}", sessionId, userId);
        }
        
        Flux<String> contentFlux = promptBuilder.stream().content();

        // 4. 处理会话初始化
        if (chatDialogueService.isNewSession(sessionId)) {
            chatTitleService.createSessionWithTitle(userId, sessionId, originQuery);
        }

        // 5. 准备响应流
        StringBuilder fullResponse = new StringBuilder();
        StringBuilder recommendations = new StringBuilder();

        // 主响应流
        Flux<String> mainResponseFlux = contentFlux
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // 尝试获取推荐结果（设置超时时间，避免阻塞）

                    try {
                        List<String> recommendationList = recommendationsFuture.get(500, TimeUnit.MILLISECONDS);
                        if (!CollectionUtils.isEmpty(recommendationList)) {
                            String formattedRecommendations = formatRecommendations(recommendationList);
                            recommendations.append(formattedRecommendations);
                            log.info("个性化推荐已获取并将追加到响应中");
                        } else {
                            log.info("个性化推荐为空");
                        }
                    } catch (Exception e) {
                        log.warn("获取个性化推荐超时或失败，将不包含推荐内容: {}", e.getMessage());
                    }

                    // 保存消息（包含推荐内容）
                    chatMessageService.saveUserMessage(sessionId, userId, originQuery, imageList);
                    String completeResponse = fullResponse + recommendations.toString();
                    chatMessageService.saveAssistantMessage(sessionId, userId, completeResponse, isRagEnhanced);

                    // 异步分析聊天内容并更新用户标签
                    analyzeChatContentAsync(userId, sessionId, originQuery, completeResponse);
                });

        // 如果有推荐内容，追加到流中
        return mainResponseFlux.concatWith(Flux.defer(() -> {
            if (!recommendations.isEmpty()) {
                return Flux.just(recommendations.toString());
            }
            return Flux.empty();
        }));
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
