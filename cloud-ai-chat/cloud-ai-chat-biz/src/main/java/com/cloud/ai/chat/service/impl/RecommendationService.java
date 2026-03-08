package com.cloud.ai.chat.service.impl;


import com.cloud.ai.chat.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐服务 - 基于用户标签生成AI推荐提问
 * @date 2025/01/16 11:30
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final AIChatService aiChatService;
    private final PromptLoader promptLoader;

    /**
     * 根据用户画像和单个标签生成个性化推荐问题
     * 供OpenFeign调用
     *
     * @param userProfile 用户简单画像（性别、年龄段、居住地、职业）
     * @param tagName     用户兴趣标签
     * @return 生成的推荐问题
     */
    public String generateQuestionsFromTags(String userProfile, String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            log.warn("标签为空，无法生成推荐问题");
            return "";
        }

        if (userProfile == null || userProfile.trim().isEmpty()) {
            userProfile = "未填写画像";
        }

        log.info("开始生成个性化推荐问题，userProfile: {}, tag: {}", userProfile, tagName);

        try {
            String prompt = promptLoader.loadPromptAndFormat("prompts/questions-from-tags-prompt.txt",
                    userProfile, tagName);

            String aiResponse = aiChatService.simpleChat(prompt);

            String question = parseGeneratedQuestion(aiResponse);
            log.info("成功生成推荐问题，tag: {}, question: {}", tagName, question);

            return question;
        } catch (Exception e) {
            log.error("调用AI生成推荐问题失败，userProfile: {}, tag: {}", userProfile, tagName, e);
            return String.format("关于%s，你知道有哪些有趣的玩法？", tagName);
        }
    }

    /**
     * 解析AI生成的单个推荐问题
     *
     * @param aiResponse AI响应内容
     * @return 解析后的推荐问题
     */
    private String parseGeneratedQuestion(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "";
        }

        return aiResponse.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty()
                        && !line.startsWith("生成")
                        && !line.startsWith("请为")
                        && !line.startsWith("推荐问题")
                        && !line.startsWith("根据"))
                .findFirst()
                .orElse(aiResponse.trim());
    }
}
