package com.cloud.ai.chat.helper;


import com.cloud.ai.chat.domain.ChatTags;
import com.cloud.ai.chat.domain.UserTags;
import com.cloud.ai.chat.repository.ChatTagsRepository;
import com.cloud.ai.chat.repository.UserTagsRepository;
import com.cloud.ai.chat.service.impl.AITagExtractionService;
import com.cloud.ai.chat.service.impl.UserTagService;
import com.cloud.ai.chat.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 聊天分析助手类
 * <p>
 * 将聊天内容分析逻辑从TagAnalysisService中抽离出来，
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAnalysisHelper {

    private final ChatTagsRepository chatTagsRepository;
    private final UserTagsRepository userTagsRepository;
    private final UserTagService userTagService;
    private final AITagExtractionService aiTagExtractionService;

    /**
     * 分析聊天内容并更新标签
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param content   聊天内容
     */
    public void analyzeChatSession(Long userId, String sessionId, String content) {
        log.info("分析聊天会话，userId: {}, sessionId: {}", userId, sessionId);

        // 1. 使用AI提取聊天标签
        List<String> chatTags = aiTagExtractionService.extractChatTagsWithAI(content);

        // 2. 获取用户基础画像标签
        List<String> profileTags = userTagService.getProfileTagNames(userId);

        // 3. 保存聊天标签
        for (String tagName : chatTags) {
            String cleanTagName = ValidationUtils.cleanTagName(tagName);
            if (cleanTagName != null) {
                ValidationUtils.validateTagName(cleanTagName);
                saveChatTag(userId, sessionId, cleanTagName);
            }
        }

        // 4. 更新用户标签
        for (String chatTag : chatTags) {
            String cleanTagName = ValidationUtils.cleanTagName(chatTag);
            if (cleanTagName != null) {
                updateUserTag(userId, cleanTagName, profileTags.contains(cleanTagName));
            }
        }
    }

    /**
     * 保存聊天标签
     */
    private void saveChatTag(Long userId, String sessionId, String tagName) {
        Optional<ChatTags> existingTag = chatTagsRepository.findByUserIdAndTagName(userId, tagName)
                .stream()
                .filter(tag -> sessionId.equals(tag.getSessionId()))
                .findFirst();

        if (existingTag.isPresent()) {
            // 更新现有标签
            ChatTags tag = existingTag.get();
            tag.setFrequency(tag.getFrequency() + 1);
            chatTagsRepository.save(tag);
        } else {
            // 创建新标签
            ChatTags tag = new ChatTags();
            tag.setUserId(userId);
            tag.setSessionId(sessionId);
            tag.setTagName(tagName);
            tag.setFrequency(1);
            tag.setSourceType("CHAT");
            chatTagsRepository.save(tag);
        }
    }

    /**
     * 更新用户标签（核心算法）
     */
    private void updateUserTag(Long userId, String tagName, boolean isOverlap) {
        Optional<UserTags> existingTag = userTagsRepository.findByUserIdAndTagName(userId, tagName);

        if (existingTag.isPresent()) {
            // 更新现有标签
            UserTags tag = existingTag.get();
            userTagService.incrementChatWeight(tag, isOverlap);

            // 如果是重叠标签，增加融合权重
            if (isOverlap) {
                userTagService.setFusionWeight(tag, tag.getFusionWeight().add(BigDecimal.ONE));
            }

            userTagsRepository.save(tag);
        } else {
            // 创建新标签
            UserTags tag = new UserTags();
            tag.setUserId(userId);
            tag.setTagName(tagName);
            tag.setSourceType("CHAT");
            tag.setChatWeight(BigDecimal.ONE);
            tag.setFusionWeight(isOverlap ? BigDecimal.ONE : BigDecimal.ZERO);
            userTagService.calculateTotalWeight(tag);
            userTagsRepository.save(tag);
        }
    }
}
