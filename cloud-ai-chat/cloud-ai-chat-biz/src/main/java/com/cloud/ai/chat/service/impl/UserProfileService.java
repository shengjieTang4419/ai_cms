package com.cloud.ai.chat.service.impl;


import com.cloud.ai.chat.domain.UserProfile;
import com.cloud.ai.chat.domain.UserTags;
import com.cloud.ai.chat.domain.UserTagsDimension;
import com.cloud.ai.chat.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像服务
 * @date 2025/10/14 15:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository profileRepository;


    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(Long userId) {
        UserProfile userProfile = profileRepository.findByUserId(userId).orElse(null);
        List<UserTags> hotTags = List.of();//userTagService.getHotTags(userId, 5);
        if (CollectionUtils.isEmpty(hotTags) || userProfile == null) {
            return userProfile;
        }
        List<UserTagsDimension> tagsDimensions = new ArrayList<>();
        for (UserTags tag : hotTags) {
            UserTagsDimension tagsDimension = UserTagsDimension.builder().tagKey(tag.getPathKey()).totalWeight(tag.getTotalWeight()).build();
            tagsDimensions.add(tagsDimension);
        }
        userProfile.setUserTagsDimensions(tagsDimensions);
        return userProfile;
    }

    /**
     * 获取用户标签
     */
    public List<UserTags> getUserTags(Long userId) {
        return List.of();//userTagService.getUserTags(userId);
    }

    /**
     * 更新聊天标签
     */
    public void updateChatTags(Long userId, List<String> chatTags) {
        // userTagService.updateChatTags(userId, chatTags);
    }
}
