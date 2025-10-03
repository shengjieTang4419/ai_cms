package com.cloud.cloud.ai.chat.controller;


import com.cloud.cloud.ai.chat.domain.ChatMessage;
import com.cloud.cloud.ai.chat.repository.ChatSessionRepository;
import com.cloud.cloud.ai.chat.service.ChatDialogueService;
import com.cloud.cloud.ai.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 对话Controller
 * @date 2025/9/27 11:36
 */
@RestController
@RequestMapping("/api/dialogue")
@RequiredArgsConstructor
public class DialogueController {

    private final ChatDialogueService chatDialogueService;
    private final ChatMessageService chatMessageService;
    private final ChatSessionRepository chatSessionRepository;

    /**
     * 获取对话历史
     */
    @GetMapping("/history/{sessionId}")
    public List<ChatMessage> getConversationHistory(@PathVariable String sessionId) {
        return chatMessageService.getSessionHistory(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @Transactional
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatSessionRepository.deleteBySessionId(sessionId);
        chatDialogueService.clearConversationMemory(sessionId);
        chatMessageService.deleteSessionMessages(sessionId);
        return ResponseEntity.ok().build();
    }
}
