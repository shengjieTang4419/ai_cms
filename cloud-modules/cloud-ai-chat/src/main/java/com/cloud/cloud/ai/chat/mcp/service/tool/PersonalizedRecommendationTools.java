package com.cloud.cloud.ai.chat.mcp.service.tool;

import com.cloud.cloud.ai.chat.domain.OccupationEntity;
import com.cloud.cloud.ai.chat.domain.UserProfile;
import com.cloud.cloud.ai.chat.domain.UserTags;
import com.cloud.cloud.ai.chat.repository.UserProfileRepository;
import com.cloud.cloud.ai.chat.repository.UserTagsRepository;
import com.cloud.cloud.ai.chat.service.OccupationService;
import com.cloud.cloud.ai.chat.provider.ModelProvider;
import com.cloud.cloud.ai.chat.provider.ModelProviderManager;
import com.cloud.cloud.ai.chat.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐工具 - 基于用户画像和聊天行为（使用SPI）
 * @date 2025/10/14 15:30
 */
@Component
@Slf4j
public class PersonalizedRecommendationTools {

    private final UserProfileRepository profileRepository;
    private final UserTagsRepository userTagsRepository;
    private final OccupationService occupationService;

    @Autowired
    @Lazy
    private ModelProviderManager providerManager;

    /**
     * 获取默认ChatClient
     */
    private ChatClient getChatClient() {
        ModelProvider defaultProvider = providerManager.getDefaultProvider();
        return defaultProvider.getChatClient();
    }

    public PersonalizedRecommendationTools(UserProfileRepository profileRepository,
                                           UserTagsRepository userTagsRepository,
                                           OccupationService occupationService) {
        this.profileRepository = profileRepository;
        this.userTagsRepository = userTagsRepository;
        this.occupationService = occupationService;
    }

    @Tool(name = "get_personalized_recommendations",
            description = "基于用户画像和聊天行为，生成个性化推荐话题和问题")
    public String getPersonalizedRecommendations(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "推荐数量，默认5") Integer limit) {

        log.info("AI 调用工具：获取个性化推荐，userId={}, limit={}", userId, limit);

        limit = ValidationUtils.validateRecommendationLimitWithDefault(limit, 5);

        try {
            // 获取用户标签（按总权重排序）
            List<UserTags> topTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId).stream().limit(limit).toList();

            if (topTags.isEmpty()) {
                return "该用户还没有足够的标签数据，无法生成个性化推荐。建议用户先完善个人资料或进行一些聊天互动。";
            }

            // 获取用户基础画像
            UserProfile profile = profileRepository.findByUserId(userId).orElse(null);

            StringBuilder result = new StringBuilder();
            result.append("🎯 个性化推荐（基于用户画像 + 聊天行为）\n\n");

            if (profile != null) {
                result.append(String.format("👤 用户画像：%s，%s岁",
                        profile.getGender() != null ? profile.getGender() : "未知",
                        profile.getAge() != null ? profile.getAge() : "未知"));

                if (profile.getOccupation() != null) {
                    String occupationName = occupationService.getByCode(profile.getOccupation())
                            .map(OccupationEntity::getName)
                            .orElse("未知职业");
                    result.append(String.format("，%s", occupationName));
                }
                result.append("\n");

                if (StringUtils.hasText(profile.getLocation())) {
                    result.append(String.format("📍 居住地：%s\n", profile.getLocation()));
                }

                if (profile.getHobbies() != null && !profile.getHobbies().isEmpty()) {
                    result.append(String.format("❤️ 爱好：%s\n\n", String.join("、", profile.getHobbies())));
                } else {
                    result.append("\n");
                }
            }

            result.append("🔥 推荐话题：\n");
            for (int i = 0; i < topTags.size(); i++) {
                UserTags tag = topTags.get(i);
                String question = generateQuestionByTagWithAI(tag.getTagName());
                result.append(String.format("%d. %s (权重: %s)\n",
                        i + 1, question, tag.getTotalWeight()));
            }

            return result.toString();

        } catch (Exception e) {
            log.error("获取个性化推荐失败", e);
            return "获取个性化推荐时发生错误：" + e.getMessage();
        }
    }

    @Tool(name = "find_similar_users",
            description = "找到兴趣相似的用户，推荐潜在的聊天伙伴")
    public String findSimilarUsers(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "相似用户数量，默认3") Integer limit) {

        log.info("AI 调用工具：查找相似用户，userId={}, limit={}", userId, limit);

        limit = ValidationUtils.validateRecommendationLimitWithDefault(limit, 3);

        try {
            // 获取用户标签
            List<UserTags> userTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId);

            if (userTags.isEmpty()) {
                return "该用户还没有足够的标签数据，无法找到相似用户。";
            }

            // 计算用户相似度（简化版）
            List<Long> similarUserIds = userTagsRepository.findSimilarUsers(userId).stream().limit(limit).toList();

            if (similarUserIds.isEmpty()) {
                return "暂时没有找到兴趣相似的用户。";
            }

            StringBuilder result = new StringBuilder();
            result.append("👥 找到的相似用户：\n\n");

            for (int i = 0; i < similarUserIds.size(); i++) {
                Long similarUserId = similarUserIds.get(i);

                // 获取相似用户的标签
                List<UserTags> similarUserTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(similarUserId);

                // 计算共同标签
                List<String> commonTags = userTags.stream()
                        .map(UserTags::getTagName)
                        .filter(tagName -> similarUserTags.stream()
                                .anyMatch(tag -> tag.getTagName().equals(tagName)))
                        .collect(Collectors.toList());

                result.append(String.format("%d. 用户 %d\n", i + 1, similarUserId));
                if (!commonTags.isEmpty()) {
                    result.append(String.format("   共同兴趣：%s\n\n", String.join("、", commonTags)));
                } else {
                    result.append("   共同兴趣：暂无\n\n");
                }
            }

            return result.toString();

        } catch (Exception e) {
            log.error("查找相似用户失败", e);
            return "查找相似用户时发生错误：" + e.getMessage();
        }
    }

    //    @Tool(name = "suggest_follow_up_topics",
//            description = "基于当前对话和用户兴趣，智能推荐相关话题")
    public List<String> suggestFollowUpTopics(
            @ToolParam(description = "当前对话主题") String currentTopic, Long userId) {

        // 自动获取当前用户ID
        log.info("AI 调用工具：推荐相关话题，userId={}, currentTopic={}", userId, currentTopic);

        try {
            // 获取用户热门标签
            List<UserTags> userTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId);

            if (userTags.isEmpty()) {
                return new ArrayList<>();
            }

            // 使用AI找到与当前话题相关的标签
            List<UserTags> relatedTags = findRelatedTagsWithAI(userTags, currentTopic);

            if (relatedTags.isEmpty()) {
                // 如果没有直接相关的，推荐用户最感兴趣的话题
                relatedTags = userTags.stream().limit(3).toList();
            }
            List<String> suggestTopics = new ArrayList<>();
            for (UserTags tag : relatedTags) {
                String suggestion = generateFollowUpSuggestionWithAI(tag.getTagName(), currentTopic);
                suggestTopics.add(suggestion);
            }
            return suggestTopics;
        } catch (Exception e) {
            log.error("推荐相关话题失败", e);
            return new ArrayList<>();
        }
    }

    @Tool(name = "analyze_user_interests",
            description = "分析用户的兴趣偏好和标签权重分布")
    public String analyzeUserInterests(
            @ToolParam(description = "用户ID") Long userId) {

        log.info("AI 调用工具：分析用户兴趣，userId={}", userId);

        try {
            // 获取用户所有标签
            List<UserTags> allTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId);

            if (allTags.isEmpty()) {
                return "该用户还没有任何标签数据。";
            }

            // 统计标签来源
            Map<String, Long> sourceStats = allTags.stream()
                    .collect(Collectors.groupingBy(UserTags::getSourceType, Collectors.counting()));

            // 计算权重分布
            BigDecimal totalBaseWeight = allTags.stream()
                    .map(UserTags::getBaseWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalChatWeight = allTags.stream()
                    .map(UserTags::getChatWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalFusionWeight = allTags.stream()
                    .map(UserTags::getFusionWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder result = new StringBuilder();
            result.append(String.format("📊 用户 %d 的兴趣分析\n\n", userId));
            result.append(String.format("- 总标签数：%d\n", allTags.size()));
            result.append(String.format("- 基础权重：%s\n", totalBaseWeight));
            result.append(String.format("- 聊天权重：%s\n", totalChatWeight));
            result.append(String.format("- 融合权重：%s\n\n", totalFusionWeight));

            result.append("标签来源分布：\n");
            sourceStats.forEach((source, count) -> {
                result.append(String.format("  • %s: %d个\n", source, count));
            });

            result.append("\nTop 5 兴趣标签：\n");
            allTags.stream().limit(5).forEach(tag -> {
                result.append(String.format("  • %s (总权重: %s, 基础: %s, 聊天: %s, 融合: %s)\n",
                        tag.getTagName(), tag.getTotalWeight(),
                        tag.getBaseWeight(), tag.getChatWeight(), tag.getFusionWeight()));
            });

            return result.toString();

        } catch (Exception e) {
            log.error("分析用户兴趣失败", e);
            return "分析用户兴趣时发生错误：" + e.getMessage();
        }
    }

    /**
     * AI生成基于标签的问题
     */
    private String generateQuestionByTagWithAI(String tagName) {
        try {
            String prompt = String.format("""
                    请为标签"%s"生成一个有趣、具体的问题，要求：
                    1. 问题要具体，不要过于宽泛
                    2. 要有吸引力，能引起用户兴趣
                    3. 问题长度控制在15-30个字
                    4. 直接返回问题，不要额外解释
                    
                    标签：%s
                    生成的问题：
                    """, tagName, tagName);

            String response = getChatClient().prompt(prompt).call().content();
            return response != null ? response.trim() : "关于 " + tagName + " 的问题";

        } catch (Exception e) {
            log.error("AI生成问题失败，使用默认问题", e);
            return "关于 " + tagName + " 的问题";
        }
    }

    /**
     * 使用AI找到与当前话题相关的标签
     */
    private List<UserTags> findRelatedTagsWithAI(List<UserTags> userTags, String currentTopic) {
        try {
            // 构建标签列表
            String tagList = userTags.stream()
                    .map(UserTags::getTagName)
                    .collect(Collectors.joining("、"));

            String prompt = String.format("""
                    请从以下用户兴趣标签中，找出与当前话题"%s"最相关的标签（最多3个）：
                    
                    用户兴趣标签：%s
                    当前话题：%s
                    
                    要求：
                    1. 基于语义相关性判断，不仅仅是关键词匹配
                    2. 考虑标签与话题的关联程度
                    3. 返回格式：标签1,标签2,标签3
                    4. 如果没有相关标签，返回空字符串
                    
                    相关标签：
                    """, currentTopic, tagList, currentTopic);

            String response = getChatClient().prompt(prompt).call().content();

            if (response == null || response.trim().isEmpty()) {
                return new ArrayList<>();
            }

            // 解析AI返回的标签
            String[] relatedTagNames = response.trim().split(",");
            List<String> relatedTagNameList = Arrays.stream(relatedTagNames)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .toList();

            // 找到对应的UserTags对象
            return userTags.stream()
                    .filter(tag -> relatedTagNameList.contains(tag.getTagName()))
                    .limit(3)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("AI查找相关标签失败，使用备用方案", e);
            // 备用方案：返回权重最高的前3个标签
            return userTags.stream().limit(3).collect(Collectors.toList());
        }
    }

    /**
     * 使用AI生成后续建议
     */
    private String generateFollowUpSuggestionWithAI(String tagName, String currentTopic) {
        try {
            String prompt = String.format("""
                    基于用户当前话题"%s"和兴趣标签"%s"，生成一个自然的后续建议问题。
                    
                    要求：
                    1. 问题要自然，像朋友间的对话
                    2. 要体现标签与当前话题的关联
                    3. 问题长度控制在20-40个字
                    4. 直接返回问题，不要额外解释
                    
                    当前话题：%s
                    兴趣标签：%s
                    生成的建议：
                    """, currentTopic, tagName, currentTopic, tagName);

            String response = getChatClient().prompt(prompt).call().content();
            return response != null ? response.trim() : "您是否想了解更多关于 " + tagName + " 的知识？";

        } catch (Exception e) {
            log.error("AI生成后续建议失败，使用默认建议", e);
            return "您是否想了解更多关于 " + tagName + " 的知识？";
        }
    }
}
