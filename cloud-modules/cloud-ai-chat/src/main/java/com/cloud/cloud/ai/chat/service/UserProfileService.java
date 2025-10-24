package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.domain.UserProfile;
import com.cloud.cloud.ai.chat.domain.UserProfileRequest;
import com.cloud.cloud.ai.chat.domain.UserTags;
import com.cloud.cloud.ai.chat.domain.UserTagsDimension;
import com.cloud.cloud.ai.chat.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final UserTagService userTagService;

    /**
     * 创建或更新用户画像
     */
    public UserProfile createOrUpdateProfile(UserProfileRequest request) {
        log.info("创建或更新用户画像，userId: {}", request.getUserId());

        Optional<UserProfile> existingProfileOpt = profileRepository.findByUserId(request.getUserId());
        UserProfile profile;

        if (existingProfileOpt.isPresent()) {
            profile = existingProfileOpt.get();
            profile.setGender(request.getGender());
            profile.setAge(request.getAge());
            profile.setLocation(request.getLocation());
            profile.setOccupation(request.getOccupation());
            profile.setHobbies(request.getHobbies());
        } else {
            profile = new UserProfile();
            profile.setUserId(request.getUserId());
            profile.setGender(request.getGender());
            profile.setAge(request.getAge());
            profile.setLocation(request.getLocation());
            profile.setOccupation(request.getOccupation());
            profile.setHobbies(request.getHobbies());
        }

        UserProfile savedProfile = profileRepository.save(profile);
        // 基于画像生成基础标签
        userTagService.generateProfileTags(request.getUserId(), savedProfile);
        return savedProfile;
    }

    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(Long userId) {
        UserProfile userProfile = profileRepository.findByUserId(userId).orElse(null);
        List<UserTags> hotTags = userTagService.getHotTags(userId, 5);
        if (CollectionUtils.isEmpty(hotTags) || userProfile == null) {
            return userProfile;
        }
        List<UserTagsDimension> tagsDimensions = new ArrayList<>();
        for (UserTags tag : hotTags) {
            UserTagsDimension tagsDimension = UserTagsDimension.builder().tagName(tag.getTagName()).totalWeight(tag.getTotalWeight()).build();
            tagsDimensions.add(tagsDimension);
        }
        userProfile.setUserTagsDimensions(tagsDimensions);
        return userProfile;
    }

    /**
     * 获取用户标签
     */
    public List<UserTags> getUserTags(Long userId) {
        return userTagService.getUserTags(userId);
    }

    /**
     * 获取用户热门标签
     */
    public List<UserTags> getHotTags(Long userId, int limit) {
        return userTagService.getHotTags(userId, limit);
    }

    /**
     * 更新聊天标签
     */
    public void updateChatTags(Long userId, List<String> chatTags) {
        userTagService.updateChatTags(userId, chatTags);
    }
}
