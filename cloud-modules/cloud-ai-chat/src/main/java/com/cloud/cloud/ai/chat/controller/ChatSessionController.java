package com.cloud.cloud.ai.chat.controller;


import com.cloud.cloud.ai.chat.domain.ChatSession;
import com.cloud.cloud.ai.chat.repository.ChatSessionRepository;
import com.cloud.cloud.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签汇总
 * @date 2025/9/24 21:57
 */
@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionRepository chatSessionRepository;

    @GetMapping("/user")
    public List<ChatSession> getUserSessions() {
        Long userId = SecurityUtils.getCurrentUserId();
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
}
