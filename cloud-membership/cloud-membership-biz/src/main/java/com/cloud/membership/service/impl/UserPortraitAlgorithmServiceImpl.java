package com.cloud.membership.service.impl;

import com.cloud.membership.constants.AlgorithmConstants;
import com.cloud.membership.domain.StereotypeTemplate;
import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.domain.UserTagsDimension;
import com.cloud.membership.service.ITemplateUserProfile;
import com.cloud.membership.service.IUserPortraitAlgorithmService;
import com.cloud.memebership.domain.TagPath;
import com.cloud.memebership.domain.UserPortraitDTO;
import com.cloud.memebership.domain.UserTagDTO;
import com.cloud.memebership.domain.ValueChat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像算法服务
 * @date 2026/1/11 11:31
 */
@Service
@RequiredArgsConstructor
public class UserPortraitAlgorithmServiceImpl implements IUserPortraitAlgorithmService {


    private final UserProfileService userProfileService;
    private final UserTagService userTagService;
    private final CredibilityService credibilityService;
    private final AttenuationService attenuationService;
    private final ITemplateUserProfile templateUserProfile;


    @Override
    public void onValueChat(ValueChat valueChat) {
        //1.load当前人物画像
        Long userId = valueChat.getUserId();
        UserProfile currentUserProfile = userProfileService.getCurrentUserProfile(userId);

        //2.快速发散补偿触达（仅前TopK次有效对话尝试）
        Integer attempts = currentUserProfile.getDivergenceAttempts() == null ? 0 : currentUserProfile.getDivergenceAttempts();
        if (attempts < AlgorithmConstants.Divergence.CHAT_TOP_K && isLowConfidence(currentUserProfile)) {
            divergence(currentUserProfile);
            currentUserProfile.setDivergenceAttempts(attempts + 1);
        }

        //3.衰减处理（在增加新标签之前，先衰减旧标签，但不衰减当前对话涉及的标签）
        List<TagPath> currentChatTagPaths = valueChat.getTagPath() == null ? List.of() : List.of(valueChat.getTagPath());
        attenuation(currentUserProfile, currentChatTagPaths);

        //3.人物标签算法
        if (valueChat.getTagPath() != null) {
            TagPath tagPath = valueChat.getTagPath();
            //获取路径匹配度
            double matchGain = credibilityService.getMatchGain(tagPath, currentUserProfile);
            userTagService.increaseTag(currentUserProfile, tagPath, matchGain);
        }

        //4.竞争淘汰（标签数量超阈值时触发）
        if (currentUserProfile.getUserTagsDimensions() != null
                && currentUserProfile.getUserTagsDimensions().size() > AlgorithmConstants.Tag.MAX_TAGS) {
            competition(currentUserProfile);
        }

        //5.坍缩检查（熵增达到阈值时触发）
        int collapseAge = currentUserProfile.getCollapseAge() != null ? currentUserProfile.getCollapseAge() : 0;
        int collapseThreshold = calculateCollapseThreshold(collapseAge);
        int entropyCount = currentUserProfile.getEntropyCount() != null ? currentUserProfile.getEntropyCount() : 0;
        if (entropyCount >= collapseThreshold) {
            collapse(currentUserProfile);
        }

        //6.持久化画像指标 + 标签沉淀
        userProfileService.saveProfile(currentUserProfile);
        userTagService.saveUserTags(currentUserProfile);
    }

    /**
     * 衰减方法：用于 UserProfile，支持排除指定标签
     * 懒加载模式：只在有对话时才检查和执行衰减
     */
    private void attenuation(UserProfile userProfile, List<TagPath> excludeTagPaths) {
        if (userProfile == null || userProfile.getUserTagsDimensions() == null) {
            return;
        }
        Set<String> excludePathKeys = excludeTagPaths.stream().map(TagPath::getKey).collect(Collectors.toSet());
        for (UserTagsDimension tag : userProfile.getUserTagsDimensions()) {
            // 跳过排除的标签，不进行衰减
            if (excludePathKeys.contains(tag.getPathKey())) {
                continue;
            }
            double currentWeight = tag.getTotalWeight();
            double finalWeight = attenuationService.calculateDecayedWeight(currentWeight, tag.getLastActiveTime(), tag.getCreatedTime());
            tag.setTotalWeight(finalWeight);
        }
    }

    /**
     * 衰减方法
     */
    @Override
    public void attenuation(Long userId) {
        UserProfile currentUserProfile = userProfileService.getCurrentUserProfile(userId);
        attenuation(currentUserProfile, List.of());
    }

    @Override
    public void competition(UserPortraitDTO userPortraitDTO, String sessionId) {
        // 外部调用入口，转换为内部处理
        UserProfile userProfile = userProfileService.getCurrentUserProfile(userPortraitDTO.getUserId());
        competition(userProfile);
        userTagService.saveUserTags(userProfile);
    }

    /**
     * 标签竞争淘汰（内部方法）
     * 参考 W-TinyLFU 思想：
     * - Protected区：稳定的高权重标签
     * - Probation区：考察中的标签
     * - 候选者 vs 考察区末位 PK，胜者入围
     */
    private void competition(UserProfile userProfile) {
        List<UserTagsDimension> allTags = userProfile.getUserTagsDimensions();
        if (allTags == null || allTags.size() <= AlgorithmConstants.Tag.MAX_TAGS) {
            return;
        }

        // 1. 按竞争评分排序（权重 + 新鲜度）
        allTags.sort(Comparator.comparingDouble(this::calculateCompetitionScore).reversed());

        // 2. 分离：Protected + Probation + Candidates
        List<UserTagsDimension> protectedTags = new ArrayList<>(allTags.subList(0, AlgorithmConstants.Tag.PROTECTED_SIZE));
        List<UserTagsDimension> probationTags = new ArrayList<>(allTags.subList(AlgorithmConstants.Tag.PROTECTED_SIZE, AlgorithmConstants.Tag.MAX_TAGS));
        List<UserTagsDimension> candidates = allTags.subList(AlgorithmConstants.Tag.MAX_TAGS, allTags.size());

        // 3. 候选者挑战考察区末位，记录熵增
        int replacementCount = 0;
        for (UserTagsDimension candidate : candidates) {
            if (probationTags.isEmpty()) {
                break;
            }
            // 找到考察区最弱的标签
            UserTagsDimension victim = probationTags.stream()
                    .min(Comparator.comparingDouble(this::calculateCompetitionScore))
                    .orElse(null);

            double candidateScore = calculateCompetitionScore(candidate);
            double victimScore = calculateCompetitionScore(victim);

            // 候选者胜出，替换末位
            if (candidateScore > victimScore) {
                probationTags.remove(victim);
                probationTags.add(candidate);
                replacementCount++; // 记录熵增事件
            }
        }

        // 4. 更新熵增计数
        if (replacementCount > 0) {
            int currentEntropy = userProfile.getEntropyCount() != null ? userProfile.getEntropyCount() : 0;
            userProfile.setEntropyCount(currentEntropy + replacementCount);
        }

        // 5. 合并最终标签列表
        List<UserTagsDimension> finalTags = new ArrayList<>();
        finalTags.addAll(protectedTags);
        finalTags.addAll(probationTags);
        userProfile.setUserTagsDimensions(finalTags);
    }

    /**
     * 计算竞争评分
     * 评分 = 权重 * (WEIGHT_FACTOR + FRESHNESS_FACTOR * 新鲜度因子)
     * 新鲜度因子 = e^(-timeSinceActive / 半衰期)
     */
    private double calculateCompetitionScore(UserTagsDimension tag) {
        double weight = tag.getTotalWeight();
        long lastActiveMs = getTimestampMillis(tag.getLastActiveTime());
        long timeSinceActive = System.currentTimeMillis() - lastActiveMs;

        // 新鲜度因子：指数衰减，7天半衰期
        double freshnessFactor = Math.exp(-(double) timeSinceActive / AlgorithmConstants.Tag.FRESHNESS_HALF_LIFE_MS);

        return weight * (AlgorithmConstants.Tag.WEIGHT_FACTOR + AlgorithmConstants.Tag.FRESHNESS_FACTOR * freshnessFactor);
    }

    /**
     * 时间戳转换（支持 LocalDateTime 和 Date）
     */
    private long getTimestampMillis(Object timeObj) {
        if (timeObj == null) {
            return System.currentTimeMillis();
        }
        if (timeObj instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) timeObj).getTime();
        }
        if (timeObj instanceof Date) {
            return ((Date) timeObj).getTime();
        }
        return System.currentTimeMillis();
    }

    @Override
    public List<UserTagDTO> collapse(UserPortraitDTO userPortraitDTO) {
        UserProfile userProfile = userProfileService.getCurrentUserProfile(userPortraitDTO.getUserId());
        List<UserTagDTO> result = collapse(userProfile);
        userTagService.saveUserTags(userProfile);
        return result;
    }

    @Override
    public void rebirth(UserProfile beforeProfile, UserProfile afterProfile) {
        if (beforeProfile == null || afterProfile == null) {
            return;
        }

        boolean majorChanged = isMajorProfileChanged(beforeProfile, afterProfile);
        if (!majorChanged) {
            if (isLowConfidence(afterProfile)) {
                divergence(afterProfile);
            }
            return;
        }

        List<UserTagsDimension> tags = afterProfile.getUserTagsDimensions();
        if (tags != null && !tags.isEmpty()) {
            tags.sort(Comparator.comparingDouble(UserTagsDimension::getTotalWeight).reversed());

            int coreSize = Math.min(AlgorithmConstants.Tag.PROTECTED_SIZE, tags.size());
            List<UserTagsDimension> core = new ArrayList<>(tags.subList(0, coreSize));
            List<UserTagsDimension> shell = new ArrayList<>(tags.subList(coreSize, tags.size()));

            double shellDecayFactor = 0.3;
            for (UserTagsDimension t : shell) {
                t.setTotalWeight(t.getTotalWeight() * shellDecayFactor);
            }

            List<UserTagsDimension> merged = new ArrayList<>();
            merged.addAll(core);
            merged.addAll(shell);
            afterProfile.setUserTagsDimensions(merged);
        }

        afterProfile.setEntropyCount(0);
        afterProfile.setConfidenceScore(0.2);
        divergence(afterProfile);
    }

    private boolean isMajorProfileChanged(UserProfile beforeProfile, UserProfile afterProfile) {
        boolean occupationChanged = !Objects.equals(beforeProfile.getOccupation(), afterProfile.getOccupation());
        boolean locationChanged = !Objects.equals(beforeProfile.getLocation(), afterProfile.getLocation());

        Set<String> beforeHobbies = beforeProfile.getHobbies() == null ? Set.of() : new HashSet<>(beforeProfile.getHobbies());
        Set<String> afterHobbies = afterProfile.getHobbies() == null ? Set.of() : new HashSet<>(afterProfile.getHobbies());
        boolean hobbiesChanged = !Objects.equals(beforeHobbies, afterHobbies);

        return occupationChanged || locationChanged || hobbiesChanged;
    }

    /**
     * 标签坍缩（内部方法）
     * 借鉴 JVM 分代年龄 + CAS 自旋思想
     * 阈值 = baseThreshold × (1 + age × ageFactor)
     */
    private List<UserTagDTO> collapse(UserProfile userProfile) {
        // 1. 计算动态坍缩阈值
        int collapseAge = userProfile.getCollapseAge() != null ? userProfile.getCollapseAge() : 0;
        int collapseThreshold = calculateCollapseThreshold(collapseAge);

        // 2. 判断是否需要坍缩
        int entropyCount = userProfile.getEntropyCount() != null ? userProfile.getEntropyCount() : 0;
        if (entropyCount < collapseThreshold) {
            return List.of(); // 未达到阈值，不坍缩
        }

        // 3. 调用 AI 模块进行标签合并分析
        List<UserTagsDimension> currentTags = userProfile.getUserTagsDimensions();
        List<UserTagDTO> mergedTags = callAIForTagMerge(currentTags);

        // 4. 更新用户画像
        if (mergedTags != null && !mergedTags.isEmpty()) {
            // 重置熵增计数
            userProfile.setEntropyCount(0);
            // 增加坍缩年龄
            userProfile.setCollapseAge(collapseAge + 1);
            // 更新标签列表（由 AI 返回的合并结果）
            // TODO: 将 mergedTags 转换回 UserTagsDimension 并更新
        }
        return mergedTags;
    }

    /**
     * 计算动态坍缩阈值
     * 借鉴 CAS 自旋时间的自适应思想
     * 阈值 = baseThreshold × (1 + age × ageFactor)
     * <p>
     * 示例：
     * age=0: 10 × (1 + 0 × 0.5) = 10  // 新用户，快速坍缩
     * age=1: 10 × (1 + 1 × 0.5) = 15
     * age=2: 10 × (1 + 2 × 0.5) = 20
     * age=5: 10 × (1 + 5 × 0.5) = 35  // 老用户，慢速坍缩
     */
    private int calculateCollapseThreshold(int collapseAge) {
        return (int) (AlgorithmConstants.Collapse.BASE_THRESHOLD * (1 + collapseAge * AlgorithmConstants.Collapse.AGE_FACTOR));
    }

    /**
     * 调用 AI 模块进行标签合并
     * AI 会分析标签相似度，返回合并建议
     * <p>
     * TODO: 对接实际的 AI 服务
     */
    private List<UserTagDTO> callAIForTagMerge(List<UserTagsDimension> currentTags) {
        // 这里需要调用 AI 模块
        // 1. 将 currentTags 转换为 AI 可识别的格式
        // 2. 调用 AI 接口分析相似度（如 70% 以上相似度的标签合并）
        // 3. 返回合并后的标签列表

        // 临时返回空列表，等待 AI 模块对接
        return List.of();
    }

    @Override
    public void divergence(Long userId) {
        UserProfile userProfile = userProfileService.getCurrentUserProfile(userId);
        divergence(userProfile);
        //更新标签
        userTagService.saveUserTags(userProfile);
        //更新人群画像 （置信度）
        userProfileService.saveProfile(userProfile);

    }

    /**
     * 快速发散
     * 刻板印象思想：基于用户基础画像快速匹配模版标签
     * 触发条件：低置信度（新用户或标签数据不足）
     */
    private void divergence(UserProfile userProfile) {
        // 1. 判断是否为低置信度用户
        if (!isLowConfidence(userProfile)) {
            return; // 高置信度用户，无需发散
        }

        // 2. 提取用户基础画像特征
        String profileFeatures = extractProfileFeatures(userProfile);
        if (profileFeatures == null || profileFeatures.isEmpty()) {
            return; // 基础画像信息不足，无法发散
        }

        // 3. 优先从模版池中匹配（降级策略 模糊匹配）
        StereotypeTemplate stereotypeTemplate = templateUserProfile.matchTemplate(userProfile);
        if (stereotypeTemplate == null) {
            return; // 模版池中无匹配，发散失败
        }

        // 4. 不再次调用AI分析特定印象，因为降级模糊匹配,because this almost can match All Scene
        // 5. 应用模版标签到用户画像
        userProfile.setUserTagsDimensions(convertToTagDimension(stereotypeTemplate));
        // 提升置信度
        userProfile.setConfidenceScore(0.5); // 基于模版的初始低置信度
    }

    private List<UserTagsDimension> convertToTagDimension(StereotypeTemplate stereotypeTemplate) {
        List<UserTagsDimension> userTagsDimensions = new ArrayList<>();
        Map<String, Double> tagWeights = stereotypeTemplate.getTagWeights();
        tagWeights.forEach((k, v) -> {
            UserTagsDimension newTag = new UserTagsDimension();
            newTag.setPathKey(k);
            newTag.setTotalWeight(v);
            TagPath tagPath = new TagPath();
            tagPath.setKey(k);
            tagPath.setPath(Arrays.asList(k.split("/")));
            newTag.setTagPath(tagPath);
            userTagsDimensions.add(newTag);
        });
        return userTagsDimensions;
    }

    /**
     * 判断是否为低置信度用户
     * 规则：collapseAge == 0 && entropyCount < 阈值
     */
    private boolean isLowConfidence(UserProfile userProfile) {
        // 方案1：基于 collapseAge 和 entropyCount
        int collapseAge = userProfile.getCollapseAge() != null ? userProfile.getCollapseAge() : 0;
        int entropyCount = userProfile.getEntropyCount() != null ? userProfile.getEntropyCount() : 0;

        // 新用户（未坍缩过）且熵增较低
        if (collapseAge == 0 && entropyCount < AlgorithmConstants.Collapse.BASE_THRESHOLD) {
            return true;
        }

        // 方案2：直接使用置信度分数
        Double confidenceScore = userProfile.getConfidenceScore();
        return confidenceScore != null && confidenceScore < AlgorithmConstants.Divergence.LOW_CONFIDENCE_THRESHOLD;
    }

    /**
     * 提取用户基础画像特征
     * 将性别、年龄、居住地、职业、爱好统一为字符串
     */
    private String extractProfileFeatures(UserProfile userProfile) {
        StringBuilder features = new StringBuilder();

        if (userProfile.getGender() != null) {
            features.append("性别:").append(userProfile.getGender()).append(",");
        }
        if (userProfile.getAge() != null) {
            features.append("年龄:").append(userProfile.getAge()).append(",");
        }
        if (userProfile.getLocation() != null) {
            features.append("居住地:").append(userProfile.getLocation()).append(",");
        }
        if (userProfile.getOccupation() != null) {
            features.append("职业:").append(userProfile.getOccupation()).append(",");
        }
        if (userProfile.getHobbies() != null && !userProfile.getHobbies().isEmpty()) {
            features.append("爱好:").append(String.join(",", userProfile.getHobbies()));
        }

        return !features.isEmpty() ? features.toString() : null;
    }

    /**
     * 调用 AI 分析刻板印象
     * AI 根据用户基础画像，返回刻板印象标签
     * <p>
     * 例如：
     * 输入："性别:男,年龄:25,居住地:北京,职业:程序员,爱好:编程,游戏"
     * 输出："tech_geek" 或 "young_professional"
     * <p>
     */
    private String callAIForStereotypeAnalysis(String profileFeatures) {
        // 这里需要调用 AI 模块
        // 1. 将 profileFeatures 发送给 AI
        // 2. AI 分析并返回刻板印象标签
        // 3. 返回标签字符串

        // 临时返回 null，等待 AI 模块对接
        return null;
    }

    @Override
    public List<String> guideTopics(UserPortraitDTO userPortraitDTO) {
        return List.of();
    }
}
