package com.cloud.cloud.ai.chat.controller;


import com.cloud.cloud.ai.chat.service.AIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;


/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/9/21 15:18
 */
@RestController
@RequestMapping("/api/aiChat")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AIChatService aiChatService;

    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query) {
        // 直接调用Service的方法
        return aiChatService.simpleChat(query);
    }

    @GetMapping("/simple/streamChat")
    public Flux<String> streamChat(
            @RequestParam("query") String query,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "imageUrl", required = false) List<String> imageUrlList) {
        return aiChatService.streamChat(query, sessionId, imageUrlList);
    }

    @GetMapping("/rag/streamChat")
    public Flux<String> streamRAGChat(
            @RequestParam("query") String query,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "imageUrl", required = false) List<String> imageUrlList) {
        return aiChatService.ragStreamChat(query, sessionId, imageUrlList);
    }
}
