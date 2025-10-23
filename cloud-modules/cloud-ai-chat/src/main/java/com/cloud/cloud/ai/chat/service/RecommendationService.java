package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.domain.UserTags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final UserProfileService userProfileService;
    private final AIChatService aiChatService;

    /**
     * 生成个性化的推荐提问
     *
     * @param userId 用户ID
     * @param limit 标签数量限制
     * @return 推荐结果，包含标签和生成的提问
     */
    public Map<String, Object> generatePersonalizedRecommendations(Long userId, int limit) {
        log.info("开始生成个性化推荐，userId: {}, limit: {}", userId, limit);

        // 1. 获取用户热门标签
        List<UserTags> hotTags = userProfileService.getHotTags(userId, limit);

        if (hotTags.isEmpty()) {
            log.warn("用户{}没有热门标签，返回空推荐", userId);
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("userId", userId);
            emptyResult.put("recommendations", hotTags);
            emptyResult.put("total", hotTags.size());
            emptyResult.put("generatedQuestions", List.of());
            return emptyResult;
        }

        // 2. 提取标签名称用于生成推荐提问
        List<String> tagNames = hotTags.stream()
                .map(UserTags::getTagName)
                .toList();

        // 3. 调用AI生成个性推荐提问
        List<String> generatedQuestions = generateQuestionsFromTags(tagNames);

        // 4. 构建响应结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("recommendations", hotTags);
        result.put("total", hotTags.size());
        result.put("generatedQuestions", generatedQuestions);

        log.info("个性化推荐生成完成，userId: {}, 标签数: {}, 提问数: {}",
                userId, hotTags.size(), generatedQuestions.size());

        return result;
    }

    /**
     * 根据用户标签生成个性推荐提问
     *
     * @param tagNames 用户标签列表
     * @return 生成的推荐提问列表
     */
    private List<String> generateQuestionsFromTags(List<String> tagNames) {
        if (tagNames.isEmpty()) {
            return List.of();
        }

        // 构建AI提示词
        String tagsStr = String.join("、", tagNames);

        String prompt = String.format("""
                根据以下用户兴趣标签，为每个标签生成一个吸引人的、个性化的推荐问题。
                要求：
                1. 每个问题都要很有趣、有吸引力，能激发用户的探索欲
                2. 问题要具体、有价值，能提供实际帮助
                3. 语言要生动活泼，带有情感色彩
                4. 不要使用过于正式的语气，要像朋友一样推荐

                用户兴趣标签：%s

                请为每个标签生成一个推荐问题，每个问题独占一行。
                例如：
                旅游 -> 3天云南日照金山全路程攻略Get！
                编程 -> 论MESI嗅探机制的作用和原理
                服装 -> 2025巴黎服装新标向

                生成的推荐问题：
                """, tagsStr);

        try {
            // 调用AI服务生成推荐提问
            String aiResponse = aiChatService.simpleChat(prompt);

            // 解析AI响应，提取生成的提问
            return parseGeneratedQuestions(aiResponse);

        } catch (Exception e) {
            log.error("调用AI生成推荐提问失败，tags: {}", tagNames, e);

            // 如果AI调用失败，返回基于模板的推荐提问
            return generateFallbackQuestions(tagNames);
        }
    }

    /**
     * 解析AI生成的推荐提问
     *
     * @param aiResponse AI响应内容
     * @return 解析后的推荐提问列表
     */
    private List<String> parseGeneratedQuestions(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return List.of();
        }

        // 按行分割，并过滤掉空行和无关内容
        return aiResponse.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("生成") && !line.startsWith("请为"))
                .toList();
    }

    /**
     * 生成备用的推荐提问（当AI调用失败时使用）
     *
     * @param tagNames 用户标签列表
     * @return 备用的推荐提问列表
     */
    private List<String> generateFallbackQuestions(List<String> tagNames) {
        return tagNames.stream()
                .map(tag -> String.format("关于%s，你知道有哪些有趣的玩法？", tag))
                .toList();
    }
}
