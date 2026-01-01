package com.cloud.ai.chat.service.impl;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.cloud.ai.chat.domain.ChatContext;
import com.cloud.ai.chat.domain.Image;
import com.cloud.ai.chat.helper.ChatAnalysisHelper;
import com.cloud.ai.chat.helper.ChatSuggestionHelper;
import com.cloud.ai.chat.provider.ModelProvider;
import com.cloud.ai.chat.util.ModelSelector;
import com.cloud.common.security.SecurityUtils;
import io.reactivex.Flowable;
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
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
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
    private final ImageService imageService;
    private final ChatSuggestionHelper chatSuggestionHelper;
    private final ChatMemory chatMemory;

    @Value("${ai.guide:true}")
    private boolean aiGuide;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

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

    public Flux<String> streamChat(String query, String sessionId, String dialogueId, List<String> imageUrlList, Boolean isWithEnableSearch, Boolean isDeepThinking, String longitude, String latitude) {
        return streamChat(query, sessionId, dialogueId, false, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList, isDeepThinking, longitude, latitude);
    }

    public Flux<String> ragStreamChat(String query, String sessionId, String dialogueId, List<String> imageUrlList, Boolean isWithEnableSearch, Boolean isDeepThinking, String longitude, String latitude) {
        boolean useThinking = Boolean.TRUE.equals(isDeepThinking);
        List<Document> relevantDocs = vectorStore.similaritySearch(query);
        if (CollectionUtils.isEmpty(relevantDocs)) {
            return streamChat(query, sessionId, dialogueId, false, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList, useThinking, longitude, latitude);
        }
        String enhancedPrompt = buildRagPrompt(query, relevantDocs);
        return streamChat(enhancedPrompt, sessionId, dialogueId, true, Boolean.TRUE.equals(isWithEnableSearch), query, imageUrlList, useThinking, longitude, latitude);
    }

    /**
     * 流式聊天核心方法
     *
     * @param query         增强后的查询（可能包含OCR文本）
     * @param sessionId     会话ID
     * @param dialogueId    对话ID（关联USER、ASSISTANT、RECOMMENDATIONS）
     * @param isRagEnhanced 是否启用RAG增强
     * @param isWebSearch   是否启用全网搜索
     * @param originQuery   原始用户查询
     * @param imageList     图片URL列表
     * @param useThinking   是否使用Thinking模型（深度思考）
     * @param longitude     当前位置经度（可选）
     * @param latitude      当前位置纬度（可选）
     * @return 响应流
     */
    private Flux<String> streamChat(String query, String sessionId, String dialogueId, boolean isRagEnhanced, boolean isWebSearch, String originQuery, List<String> imageList, boolean useThinking, String longitude, String latitude) {
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("开始流式对话，userId: {}, sessionId: {}, query: {}, images: {}, rag={}, webSearch={}, deepThinking={}",
                userId, sessionId, originQuery, imageList != null ? imageList.size() : 0, isRagEnhanced, isWebSearch, useThinking);

        // 1. 处理会话初始化（在流开始前完成）
        initializeSessionIfNeeded(userId, sessionId, originQuery);

        //2.采用异步方式 预先生成话题引导
        if (aiGuide) {
            chatSuggestionHelper.asyncCreateSuggestion(query, sessionId, dialogueId, userId);
        }

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
        // 7. 处理响应流：收集内容、保存消息
        return processResponseStream(contentFlux, userId, sessionId, dialogueId, originQuery, imageList, isRagEnhanced);
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
        // 如果启用思考模型，直接使用DashScope SDK来获取完整的思考过程
        if (isThinking) {
            log.info("为本次请求启用DashScope思考模型功能，sessionId: {}", sessionId);
            // 获取优先级最高的支持Thinking的模型
            ModelProvider thinkingProvider = modelSelector.getProviderManager().getThinkingProvider();
            String modelName = thinkingProvider.getModelName();
            log.info("使用Thinking模型: {} ({})", thinkingProvider.getDisplayName(), modelName);
            return buildThinkingStreamWithDashScope(enhancedQuery, sessionId, modelName, isWebSearch);
        }

        // 非思考模型，使用Spring AI的ChatClient
        var promptBuilder = chatClient.prompt(enhancedQuery)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId));

        // 构建请求选项
        var optionsBuilder = DashScopeChatOptions.builder();

        // 当启用全网搜索时，设置enableSearch选项
        if (isWebSearch) {
            optionsBuilder.withEnableSearch(true);
            log.info("为本次请求启用DashScope全网搜索功能，sessionId: {}", sessionId);
        }

        DashScopeChatOptions requestOptions = optionsBuilder.build();
        promptBuilder = promptBuilder.options(requestOptions);

        return promptBuilder.stream().content();
    }

    /**
     * 使用DashScope SDK直接调用思考模型，获取思考过程和最终回复
     * <p>
     * 返回格式使用特殊标记，方便前端解析：
     * [THINKING_START]思考过程内容[THINKING_END]
     * [ANSWER_START]最终回答内容[ANSWER_END]
     *
     * @param userQuery 用户查询
     * @param sessionId 会话ID
     * @param modelName 模型名称
     * @param enableSearch 是否启用全网搜索
     * @return 包含思考过程和最终回复的流
     */
    private Flux<String> buildThinkingStreamWithDashScope(String userQuery, String sessionId, String modelName, boolean enableSearch) {
        return Flux.defer(() -> {
            try {
                Generation gen = new Generation();
                // 构建消息列表（包含历史对话）
                List<Message> messages = buildMessagesFromHistory(userQuery, sessionId);

                // 构建请求参数
                GenerationParam param = GenerationParam.builder()
                        .apiKey(apiKey)
                        .model(modelName)
                        .enableThinking(true)           // 启用思考模式
                        .incrementalOutput(true)        // 启用增量输出（流式）
                        .resultFormat("message")        // 使用message格式
                        .enableSearch(enableSearch)
                        .messages(messages)
                        .build();

                // 流式调用 - 将RxJava的Flowable转换为Reactor的Flux
                Flowable<GenerationResult> rxFlowable = gen.streamCall(param);

                boolean[] thinkingStarted = {false};
                boolean[] answerStarted = {false};

                // 将RxJava Flowable转换为Reactor Flux，实现真正的流式输出
                return Flux.from(rxFlowable)
                        .flatMap(message -> {
                            try {
                                String reasoning = message.getOutput().getChoices().get(0).getMessage().getReasoningContent();
                                String content = message.getOutput().getChoices().get(0).getMessage().getContent();

                                List<String> chunks = new ArrayList<>();

                                // 处理思考过程
                                if (reasoning != null && !reasoning.isEmpty()) {
                                    if (!thinkingStarted[0]) {
                                        chunks.add("[THINKING_START]");
                                        thinkingStarted[0] = true;
                                    }
                                    chunks.add(reasoning);
                                }

                                // 如果思考过程结束，添加结束标记
                                if (thinkingStarted[0] && (content != null && !content.isEmpty()) && !answerStarted[0]) {
                                    chunks.add("[THINKING_END]");
                                }

                                // 处理最终回复
                                if (content != null && !content.isEmpty()) {
                                    if (!answerStarted[0]) {
                                        chunks.add("[ANSWER_START]");
                                        answerStarted[0] = true;
                                    }
                                    chunks.add(content);
                                }

                                return Flux.fromIterable(chunks);
                            } catch (Exception e) {
                                log.error("处理思考模型响应时出错", e);
                                return Flux.error(e);
                            }
                        })
                        .concatWith(Flux.defer(() -> {
                            // 在流结束时添加结束标记
                            if (answerStarted[0]) {
                                log.info("思考模型响应完成，sessionId: {}", sessionId);
                                return Flux.just("[ANSWER_END]");
                            }
                            return Flux.empty();
                        }));

            } catch (NoApiKeyException | ApiException | InputRequiredException e) {
                log.error("调用DashScope思考模型失败", e);
                return Flux.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从ChatMemory构建消息列表（包含历史对话）
     */
    private List<Message> buildMessagesFromHistory(String userQuery, String sessionId) {
        List<Message> messages = new ArrayList<>();

        // 从ChatMemory获取历史对话
        List<org.springframework.ai.chat.messages.Message> historyMessages = chatMemory.get(sessionId);

        // 转换为DashScope的Message格式
        for (org.springframework.ai.chat.messages.Message msg : historyMessages) {
            String role = switch (msg.getMessageType()) {
                case USER -> Role.USER.getValue();
                case ASSISTANT -> Role.ASSISTANT.getValue();
                case SYSTEM -> Role.SYSTEM.getValue();
                default -> Role.USER.getValue();
            };

            messages.add(Message.builder()
                    .role(role)
                    .content(msg.getText())
                    .build());
        }

        // 添加当前用户消息
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(userQuery)
                .build());

        return messages;
    }


    /**
     * 处理响应流：收集内容、追加推荐、保存消息
     */
    private Flux<String> processResponseStream(Flux<String> contentFlux,
                                               Long userId, String sessionId, String dialogueId, String originQuery,
                                               List<String> imageList, boolean isRagEnhanced) {
        StringBuilder fullResponse = new StringBuilder();

        // 主响应流：收集AI响应内容
        Flux<String> mainResponseFlux = contentFlux
                .doOnNext(fullResponse::append)
                .doOnError(error -> log.error("流式响应处理出错，userId: {}, sessionId: {}", userId, sessionId, error));

        // 合并主响应流和推荐流（concatWith 确保主响应流完成后才订阅推荐流）
        return mainResponseFlux
                .doOnComplete(() -> {
                    String completeResponse = fullResponse.toString();
                    // 保存消息
                    saveMessages(userId, sessionId, dialogueId, originQuery, imageList, completeResponse, isRagEnhanced);
                    // 异步分析聊天内容并更新用户标签
                    analyzeChatContentAsync(userId, sessionId, originQuery, completeResponse);
                });
    }

    /**
     * 保存用户消息和助手消息
     * 使用前端传入的dialogueId关联一轮完整对话
     */
    private void saveMessages(Long userId, String sessionId, String dialogueId, String originQuery,
                              List<String> imageList, String completeResponse, boolean isRagEnhanced) {
        try {
            chatMessageService.saveUserMessage(sessionId, dialogueId, userId, originQuery, imageList);
            chatMessageService.saveAssistantMessage(sessionId, dialogueId, userId, completeResponse, isRagEnhanced);
            log.debug("消息已保存，sessionId: {}, dialogueId: {}", sessionId, dialogueId);
        } catch (Exception e) {
            log.error("保存消息失败，sessionId: {}, dialogueId: {}", sessionId, dialogueId, e);
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
