package com.cloud.membership.service.impl;


import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.domain.UserTags;
import com.cloud.membership.domain.UserTagsDimension;
import com.cloud.membership.repository.UserTagsRepository;
import com.cloud.memebership.domain.TagPath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签服务
 * @date 2026/1/11 19:26
 */
@Component
@RequiredArgsConstructor
public class UserTagService {

    private final UserTagsRepository userTagsRepository;

    public List<UserTags> getHotTags(Long userId, int limit) {
        List<UserTags> allTags = userTagsRepository.findByUserIdOrderByTotalWeightDesc(userId);
        return allTags.size() > limit ? allTags.subList(0, limit) : allTags;
    }

    public void saveUserTags(UserProfile currentUserProfile) {
        List<UserTags> userTagsList = new ArrayList<>();
        for (UserTagsDimension tagsDimension : currentUserProfile.getUserTagsDimensions()) {
            boolean isCreate = tagsDimension.getId() == null;
            UserTags userTags = new UserTags();
            userTags.setId(tagsDimension.getId());
            userTags.setUserId(currentUserProfile.getUserId());
            userTags.setPathKey(tagsDimension.getPathKey());
            if (isCreate) {
                userTags.setCreatedAt(LocalDateTime.now());
            }
            userTags.setLastUpdated(LocalDateTime.now());
            userTags.setTotalWeight(tagsDimension.getTotalWeight());
            userTagsList.add(userTags);
        }
        userTagsRepository.saveAll(userTagsList);
    }

    public void increaseTag(UserProfile userProfile, TagPath newPath, double gain) {
        String key = newPath.getKey();
        List<UserTagsDimension> userTagsDimensions = userProfile.getUserTagsDimensions();
        Map<String, UserTagsDimension> tagMap = userTagsDimensions.stream().collect(Collectors.toMap(UserTagsDimension::getPathKey, Function.identity()));
        UserTagsDimension tag = tagMap.get(key);

        if (tag == null) {
            tag = createNewTag(newPath, gain);
            tagMap.put(key, tag);
        } else {
            strengthenExistingTag(tag, gain);
        }
    }

    private UserTagsDimension createNewTag(TagPath path, double gain) {
        // ⚠️ 新标签：低置信起步 gain * 0.6 ==> gain 是“证据强度”,但这是第一次出现,不能直接等价为“兴趣强度”
        return UserTagsDimension.builder().tagPath(path)
                .pathKey(path.getKey())
                .totalWeight(Math.min(1.0, gain * 0.6))
                .lastActiveTime(LocalDateTime.now()).build();
    }

    private void strengthenExistingTag(UserTagsDimension tag, double gain) {

        double current = tag.getTotalWeight();

        // 非线性增长，避免爆炸
        double delta = gain * (1.0 - sigmoid(current));

        tag.setTotalWeight(current + delta);
        tag.setLastActiveTime(LocalDateTime.now());
    }

    /**
     * 防止权重无限增大
     * 这一步非常关键，它保证了：
     * •	早期增长快（探索）
     * •	后期增长慢（稳定）
     * •	永远不会“一个标签统治一切”
     *
     * @param x
     * @return
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }


}
