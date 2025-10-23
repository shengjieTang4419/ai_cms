package com.cloud.cloud.ai.chat.service;


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
    private final TagAnalysisService tagAnalysisService;


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
        Flux<String> contentFlux = chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream().content();
        if (chatDialogueService.isNewSession(sessionId)) {
            chatTitleService.createSessionWithTitle(userId, sessionId, query);
        }
        StringBuilder fullResponse = new StringBuilder();
        return contentFlux
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    chatMessageService.saveUserMessage(sessionId, userId, originQuery);
                    chatMessageService.saveAssistantMessage(sessionId, userId, fullResponse.toString(), isRagEnhanced);
                    
                    // 异步分析聊天内容并更新用户标签
                    analyzeChatContentAsync(userId, sessionId, originQuery, fullResponse.toString());
                });
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
            tagAnalysisService.analyzeChatSession(userId, sessionId, combinedContent);
            
            log.info("聊天内容分析完成，userId: {}, sessionId: {}", userId, sessionId);
        } catch (Exception e) {
            log.error("聊天内容分析失败，userId: {}, sessionId: {}", userId, sessionId, e);
        }
    }
}
