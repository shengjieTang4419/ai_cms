package com.cloud.ai.chat.controller;


import com.cloud.ai.chat.domain.ChatMessage;
import com.cloud.ai.chat.repository.ChatSessionRepository;
import com.cloud.ai.chat.service.impl.ChatDialogueService;
import com.cloud.ai.chat.service.impl.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 对话Controller
 * @date 2025/9/27 11:36
 */
@RestController
@RequestMapping("/dialogue")
@RequiredArgsConstructor
public class DialogueController {

    private final ChatDialogueService chatDialogueService;
    private final ChatMessageService chatMessageService;
    private final ChatSessionRepository chatSessionRepository;

    /**
     * 获取对话历史
     */
    @GetMapping("/history/{sessionId}")
    public List<ConversationTurn> getConversationHistory(@PathVariable String sessionId) {
        List<ChatMessage> history = chatMessageService.getSessionHistory(sessionId);
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<ChatMessage>> grouped = history.stream()
                .collect(Collectors.groupingBy(ChatMessage::getDialogueId));

        return grouped.entrySet().stream()
                .sorted((a, b) -> {
                    ChatMessage aFirst = a.getValue().stream().findFirst().orElse(null);
                    ChatMessage bFirst = b.getValue().stream().findFirst().orElse(null);
                    if (aFirst == null || bFirst == null) {
                        return 0;
                    }
                    if (aFirst.getCreatedAt() == null || bFirst.getCreatedAt() == null) {
                        return 0;
                    }
                    return aFirst.getCreatedAt().compareTo(bFirst.getCreatedAt());
                })
                .map(entry -> {
                    String dialogueId = entry.getKey();
                    ChatMessage user = null;
                    ChatMessage assistant = null;
                    ChatMessage recommendations = null;
                    for (ChatMessage m : entry.getValue()) {
                        if (m == null || m.getMessageType() == null) {
                            continue;
                        }
                        if (ChatMessage.MessageType.USER.equals(m.getMessageType())) {
                            user = m;
                        } else if (ChatMessage.MessageType.ASSISTANT.equals(m.getMessageType())) {
                            assistant = m;
                        } else if (ChatMessage.MessageType.RECOMMENDATIONS.equals(m.getMessageType())) {
                            recommendations = m;
                        }
                    }
                    return new ConversationTurn(dialogueId, user, assistant, recommendations);
                })
                .collect(Collectors.toList());
    }

    public static class ConversationTurn {
        public String dialogueId;
        public ChatMessage user;
        public ChatMessage assistant;
        public ChatMessage recommendations;

        public ConversationTurn(String dialogueId, ChatMessage user, ChatMessage assistant, ChatMessage recommendations) {
            this.dialogueId = dialogueId;
            this.user = user;
            this.assistant = assistant;
            this.recommendations = recommendations;
        }
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
