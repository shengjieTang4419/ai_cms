package com.cloud.ai.chat.service.impl;

import com.cloud.memebership.api.RemoteUserPortraitAlgorithmFeignService;
import com.cloud.memebership.domain.TagPath;
import com.cloud.memebership.domain.ValueChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 对话分析类
 * @date 2026/2/1 16:21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatAnalysisService {

    private final RemoteUserPortraitAlgorithmFeignService remoteUserPortraitAlgorithmFeignService;
    private final AITagExtractionService aiTagExtractionService;

    @Async("chatTaskExecutor")
    public void analysis(Long userId, String query, String sessionId, String dialogueId) {
        ValueChat valueChat = new ValueChat();
        valueChat.setUserId(userId);
        valueChat.setQuery(query);
        valueChat.setSessionId(sessionId);
        valueChat.setDialogueId(dialogueId);

        TagPath tagPath = null;
        try {
            tagPath = aiTagExtractionService.extractChatTagsWithAI(query);
        } catch (Exception e) {
            log.error("AI提取TagPath失败，继续执行远程调用，userId: {}, sessionId: {}", userId, sessionId, e);
        }

        valueChat.setTagPath(tagPath);
        remoteUserPortraitAlgorithmFeignService.onValueChat(valueChat);
    }


    public void analyzeChatContentAsync(Long userId, String sessionId, String userQuery) {
        try {
            // 分析聊天内容
            aiTagExtractionService.extractChatTagsWithAI(userQuery);
        } catch (Exception e) {
        }
    }
}
