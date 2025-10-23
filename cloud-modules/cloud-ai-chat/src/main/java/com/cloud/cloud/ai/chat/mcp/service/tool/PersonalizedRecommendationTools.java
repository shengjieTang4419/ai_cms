package com.cloud.cloud.ai.chat.mcp.service.tool;

import com.cloud.cloud.ai.chat.domain.UserProfile;
import com.cloud.cloud.ai.chat.domain.UserTags;
import com.cloud.cloud.ai.chat.repository.UserProfileRepository;
import com.cloud.cloud.ai.chat.repository.UserTagsRepository;
import com.cloud.cloud.ai.chat.service.TagAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 个性化推荐工具 - 基于用户画像和聊天行为
 * @date 2025/10/14 15:30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersonalizedRecommendationTools {

    private final UserProfileRepository profileRepository;
    private final UserTagsRepository userTagsRepository;
    private final TagAnalysisService tagAnalysisService;

    @Tool(name = "get_personalized_recommendations", 
          description = "基于用户画像和聊天行为，生成个性化推荐话题和问题")
    public String getPersonalizedRecommendations(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "推荐数量，默认5") Integer limit) {
        
        log.info("AI 调用工具：获取个性化推荐，userId={}, limit={}", userId, limit);
        
        if (limit == null || limit <= 0) {
            limit = 5;
        }

        try {
            // 获取用户标签（按总权重排序）
            List<UserTags> topTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());

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
                    result.append(String.format("，%s", getOccupationName(profile.getOccupation())));
                }
                result.append("\n");
                
                if (profile.getLocation() != null) {
                    result.append(String.format("📍 居住地：%s\n", getLocationName(profile.getLocation())));
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
                result.append(String.format("%d. %s (权重: %d)\n", 
                    i + 1, generateQuestionByTag(tag.getTagName()), tag.getTotalWeight()));
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
        
        if (limit == null || limit <= 0) {
            limit = 3;
        }

        try {
            // 获取用户标签
            List<UserTags> userTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId);

            if (userTags.isEmpty()) {
                return "该用户还没有足够的标签数据，无法找到相似用户。";
            }

            // 计算用户相似度（简化版）
            List<Long> similarUserIds = tagAnalysisService.findSimilarUsers(userId, limit);

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
            int totalBaseWeight = allTags.stream().mapToInt(UserTags::getBaseWeight).sum();
            int totalChatWeight = allTags.stream().mapToInt(UserTags::getChatWeight).sum();
            int totalFusionWeight = allTags.stream().mapToInt(UserTags::getFusionWeight).sum();

            StringBuilder result = new StringBuilder();
            result.append(String.format("📊 用户 %d 的兴趣分析\n\n", userId));
            result.append(String.format("- 总标签数：%d\n", allTags.size()));
            result.append(String.format("- 基础权重：%d\n", totalBaseWeight));
            result.append(String.format("- 聊天权重：%d\n", totalChatWeight));
            result.append(String.format("- 融合权重：%d\n\n", totalFusionWeight));

            result.append("标签来源分布：\n");
            sourceStats.forEach((source, count) -> {
                result.append(String.format("  • %s: %d个\n", source, count));
            });

            result.append("\nTop 5 兴趣标签：\n");
            allTags.stream().limit(5).forEach(tag -> {
                result.append(String.format("  • %s (总权重: %d, 基础: %d, 聊天: %d, 融合: %d)\n",
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
     * 根据标签生成推荐问题
     */
    private String generateQuestionByTag(String tagName) {
        Map<String, String> tagQuestions = new HashMap<>();
        tagQuestions.put("编程", "如何学习 Spring Boot 框架？");
        tagQuestions.put("技术", "最新的技术趋势有哪些？");
        tagQuestions.put("软件开发", "如何提高代码质量？");
        tagQuestions.put("天气", "今天天气怎么样？");
        tagQuestions.put("旅游", "推荐一些适合旅游的地方");
        tagQuestions.put("美食", "有什么好吃的推荐吗？");
        tagQuestions.put("运动", "如何制定健身计划？");
        tagQuestions.put("摄影", "如何拍出更好的照片？");
        tagQuestions.put("音乐", "推荐一些好听的歌曲");
        tagQuestions.put("电影", "有什么好看的电影推荐？");
        tagQuestions.put("读书", "推荐一些值得读的书");
        tagQuestions.put("游戏", "有什么好玩的游戏推荐？");
        tagQuestions.put("设计", "如何提升设计技能？");
        tagQuestions.put("教育", "如何高效学习？");
        tagQuestions.put("健康", "如何保持身体健康？");
        tagQuestions.put("商务", "如何提升沟通技巧？");
        tagQuestions.put("金融", "如何理财投资？");
        tagQuestions.put("媒体", "如何制作优质内容？");
        tagQuestions.put("法律", "如何了解法律知识？");

        return tagQuestions.getOrDefault(tagName, "关于 " + tagName + " 的问题");
    }

    /**
     * 获取职业名称
     */
    private String getOccupationName(Integer occupation) {
        Map<Integer, String> occupationMap = Map.of(
            1, "程序员", 2, "设计师", 3, "教师", 4, "医生",
            5, "销售", 6, "金融", 7, "媒体", 8, "法律"
        );
        return occupationMap.getOrDefault(occupation, "未知职业");
    }

    /**
     * 获取居住地名称
     */
    private String getLocationName(Integer location) {
        // 这里可以集成城市信息表
        return "城市" + location;
    }
}
