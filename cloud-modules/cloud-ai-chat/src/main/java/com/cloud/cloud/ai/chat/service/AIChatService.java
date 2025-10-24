package com.cloud.cloud.ai.chat.service;


import com.cloud.cloud.ai.chat.helper.ChatAnalysisHelper;
import com.cloud.cloud.ai.chat.mcp.service.tool.PersonalizedRecommendationTools;
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
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: AiService
 * @date 2025/9/21 15:16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatService {

    private final ChatClient chatClient;
    private final ChatTitleService chatTitleService;
    private final ChatDialogueService chatDialogueService;
    private final ChatMessageService chatMessageService;
    private final PgVectorStore vectorStore;
    private final ChatAnalysisHelper chatAnalysisHelper;
    private final PersonalizedRecommendationTools recommendationTools;


    public String simpleChat(String query) {
        return chatClient.prompt(query).call().content();
    }

    public Flux<String> streamChat(String query, String sessionId) {
        return streamChat(query, sessionId, false, query);
    }

    public Flux<String> ragStreamChat(String query, String sessionId) {
        List<Document> relevantDocs = vectorStore.similaritySearch(query);
        if (CollectionUtils.isEmpty(relevantDocs)) {
            return streamChat(query, sessionId, false, query);
        }
        String enhancedPrompt = buildRagPrompt(query, relevantDocs);
        return streamChat(enhancedPrompt, sessionId, true, query);
    }

    private Flux<String> streamChat(String query, String sessionId, boolean isRagEnhanced, String originQuery) {
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("异步生成个性化推荐，userId: {}, sessionId: {}, query: {}", userId, sessionId, originQuery);
        CompletableFuture<List<String>> recommendationsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return recommendationTools.suggestFollowUpTopics(originQuery, userId);
            } catch (Exception e) {
                log.error("生成个性化推荐失败，userId: {}, sessionId: {}", userId, sessionId, e);
                return null;
            }
        });

        Flux<String> contentFlux = chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream().content();
        if (chatDialogueService.isNewSession(sessionId)) {
            chatTitleService.createSessionWithTitle(userId, sessionId, query);
        }
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
                    chatMessageService.saveUserMessage(sessionId, userId, originQuery);
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
