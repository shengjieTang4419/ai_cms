package com.cloud.ai.chat.helper;

import com.cloud.ai.chat.mcp.tools.office.PersonalizedRecommendationMcpTool;
import com.cloud.ai.chat.service.impl.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天建议助手 - 处理异步话题推荐生成
 *
 * @author shengjie.tang
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSuggestionHelper {

    private final PersonalizedRecommendationMcpTool personalizedRecommendationMcpTool;
    private final ChatMessageService chatMessageService;

    /**
     * 异步生成话题建议
     *
     * @param query      用户查询
     * @param sessionId  会话ID
     * @param dialogueId 对话ID
     * @param userId     用户ID
     */
    @Async
    public void asyncCreateSuggestion(String query, String sessionId, String dialogueId, Long userId) {
        try {
            List<String> suggestFollowUpTopics = personalizedRecommendationMcpTool.suggestFollowUpTopics(query);
            chatMessageService.saveRecommendationMessage(sessionId, dialogueId, userId, suggestFollowUpTopics);
            log.debug("异步话题建议生成完成，sessionId: {}, dialogueId: {}", sessionId, dialogueId);
        } catch (Exception e) {
            log.error("异步生成话题建议失败，sessionId: {}, dialogueId: {}", sessionId, dialogueId, e);
        }
    }
}
