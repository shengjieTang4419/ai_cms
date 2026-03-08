package com.cloud.ai.chat.service.impl;

import com.cloud.ai.chat.api.domain.StereotypeTagRequest;
import com.cloud.ai.chat.api.domain.StereotypeTagResponse;
import com.cloud.ai.chat.provider.ModelProvider;
import com.cloud.ai.chat.provider.ModelProviderManager;
import com.cloud.ai.chat.util.PromptLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象标签生成服务
 * 基于用户特征（性别、年龄段、职业、居住地、爱好）通过AI生成刻板印象标签
 * @date 2026/2/5 16:30
 */
@Service
@Slf4j
public class StereotypeTagService {

    private final ModelProviderManager providerManager;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    public StereotypeTagService(ModelProviderManager providerManager,
                                ObjectMapper objectMapper,
                                PromptLoader promptLoader) {
        this.providerManager = providerManager;
        this.objectMapper = objectMapper;
        this.promptLoader = promptLoader;
    }

    /**
     * 获取默认ChatClient
     */
    private ChatClient getChatClient() {
        ModelProvider defaultProvider = providerManager.getDefaultProvider();
        return defaultProvider.getChatClient();
    }

    /**
     * 根据用户特征生成刻板印象标签
     */
    public StereotypeTagResponse generateStereotypeTags(StereotypeTagRequest request) {
        if (request == null) {
            return StereotypeTagResponse.fail("请求参数不能为空");
        }

        try {
            // 1. 构建用户特征描述
            String userFeatures = buildUserFeaturesDescription(request);
            if (!StringUtils.hasText(userFeatures)) {
                return StereotypeTagResponse.fail("用户特征信息不足");
            }

            // 2. 构建prompt并调用AI
            String prompt = promptLoader.loadPromptAndFormat("prompts/stereotype-tag-prompt.txt", userFeatures);
            ModelProvider defaultProvider = providerManager.getDefaultProvider();
            String response = defaultProvider.simpleCall(prompt);
            // 3. 解析AI响应
            return parseAIResponse(Objects.requireNonNull(response));

        } catch (Exception e) {
            log.error("AI生成刻板印象标签失败", e);
            return StereotypeTagResponse.fail("AI生成标签失败: " + e.getMessage());
        }
    }

    /**
     * 构建用户特征描述
     */
    private String buildUserFeaturesDescription(StereotypeTagRequest request) {
        StringBuilder features = new StringBuilder();

        if (StringUtils.hasText(request.getGender())) {
            features.append("- 性别：").append(request.getGender()).append("\n");
        }

        if (StringUtils.hasText(request.getAgeRangeLabel())) {
            features.append("- 年龄段：").append(request.getAgeRangeLabel());
            if (request.getMinAge() != null && request.getMaxAge() != null) {
                features.append("（").append(request.getMinAge()).append("-").append(request.getMaxAge()).append("岁）");
            }
            features.append("\n");
        }

        if (StringUtils.hasText(request.getOccupationName())) {
            features.append("- 职业：").append(request.getOccupationName()).append("\n");
        }

        if (StringUtils.hasText(request.getLocation())) {
            features.append("- 居住地：").append(request.getLocation()).append("\n");
        }

        if (!CollectionUtils.isEmpty(request.getHobbies())) {
            features.append("- 爱好：").append(String.join("、", request.getHobbies())).append("\n");
        }

        return features.toString();
    }

    /**
     * 解析AI响应
     */
    @SuppressWarnings("unchecked")
    private StereotypeTagResponse parseAIResponse(String response) {
        try {
            String trimmed = response.trim();

            // 尝试提取JSON部分（处理AI可能返回的额外文本）
            int jsonStart = trimmed.indexOf('{');
            int jsonEnd = trimmed.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                trimmed = trimmed.substring(jsonStart, jsonEnd + 1);
            }

            Map<String, Object> result = objectMapper.readValue(trimmed, Map.class);

            // 解析tags
            List<String> tags = new ArrayList<>();
            Object tagsObj = result.get("tags");
            if (tagsObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) {
                        String tag = String.valueOf(item).trim();
                        if (StringUtils.hasText(tag) && tag.contains("/")) {
                            tags.add(tag);
                        }
                    }
                }
            }

            if (tags.isEmpty()) {
                return StereotypeTagResponse.fail("AI未返回有效标签");
            }

            // 解析weights
            Map<String, Double> tagWeights = new HashMap<>();
            Object weightsObj = result.get("weights");
            if (weightsObj instanceof Map<?, ?> weightsMap) {
                for (Map.Entry<?, ?> entry : weightsMap.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    Double value = parseDouble(entry.getValue());
                    if (StringUtils.hasText(key) && value != null) {
                        tagWeights.put(key, value);
                    }
                }
            }

            // 如果没有权重，生成默认权重
            if (tagWeights.isEmpty()) {
                for (int i = 0; i < tags.size(); i++) {
                    tagWeights.put(tags.get(i), 0.8 - (i * 0.1));
                }
            }

            // 解析description
            String description = "";
            Object descObj = result.get("description");
            if (descObj != null) {
                description = String.valueOf(descObj);
            }

            return StereotypeTagResponse.success(tags, tagWeights, description);

        } catch (JsonProcessingException e) {
            log.warn("解析AI响应JSON失败: {}", response, e);
            return StereotypeTagResponse.fail("解析AI响应失败");
        }
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
