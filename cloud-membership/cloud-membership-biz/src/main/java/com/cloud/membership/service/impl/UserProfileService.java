package com.cloud.membership.service.impl;

import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.domain.UserProfileRequest;
import com.cloud.membership.domain.UserTags;
import com.cloud.membership.domain.UserTagsDimension;
import com.cloud.membership.event.UserProfileUpsertedEvent;
import com.cloud.membership.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建或更新用户画像
     */
    public UserProfile createOrUpdateProfile(UserProfileRequest request) {
        log.info("创建或更新用户画像，userId: {}", request.getUserId());

        Optional<UserProfile> existingProfileOpt = profileRepository.findLatestValidProfile(request.getUserId());
        UserProfile profile;

        UserProfile beforeProfile = null;
        boolean created;

        if (existingProfileOpt.isPresent()) {
            profile = existingProfileOpt.get();
            created = false;
            beforeProfile = snapshotBase(profile);
            profile.setGender(request.getGender());
            profile.setAge(request.getAge());
            profile.setLocation(request.getLocation());
            profile.setOccupation(request.getOccupation());
            profile.setOccupationName(request.getOccupationName());
            profile.setHobbies(request.getHobbies());
        } else {
            profile = new UserProfile();
            created = true;
            profile.setUserId(request.getUserId());
            profile.setGender(request.getGender());
            profile.setAge(request.getAge());
            profile.setLocation(request.getLocation());
            profile.setOccupation(request.getOccupation());
            profile.setOccupationName(request.getOccupationName());
            profile.setHobbies(request.getHobbies());
        }

        UserProfile savedProfile = profileRepository.save(profile);
        eventPublisher.publishEvent(new UserProfileUpsertedEvent(created, beforeProfile, savedProfile));
        return savedProfile;
    }

    private UserProfile snapshotBase(UserProfile profile) {
        UserProfile snapshot = new UserProfile();
        snapshot.setId(profile.getId());
        snapshot.setUserId(profile.getUserId());
        snapshot.setGender(profile.getGender());
        snapshot.setAge(profile.getAge());
        snapshot.setLocation(profile.getLocation());
        snapshot.setOccupation(profile.getOccupation());
        snapshot.setHobbies(profile.getHobbies() == null ? null : new ArrayList<>(profile.getHobbies()));
        snapshot.setEntropyCount(profile.getEntropyCount());
        snapshot.setCollapseAge(profile.getCollapseAge());
        snapshot.setConfidenceScore(profile.getConfidenceScore());
        snapshot.setDivergenceAttempts(profile.getDivergenceAttempts());
        return snapshot;
    }

    /**
     * 获取用户画像
     */
    public UserProfile getCurrentUserProfile(Long userId) {
        UserProfile userProfile = profileRepository.findLatestValidProfile(userId).orElse(null);
        List<UserTags> hotTags = userTagService.getHotTags(userId, 50);
        if (CollectionUtils.isEmpty(hotTags) || userProfile == null) {
            return userProfile;
        }
        List<UserTagsDimension> tagsDimensions = new ArrayList<>();
        for (UserTags tag : hotTags) {
            UserTagsDimension tagsDimension = UserTagsDimension.builder()
                    .id(tag.getId())
                    .pathKey(tag.getPathKey())
                    .tagPath(tag.getTagPath())
                    .totalWeight(tag.getTotalWeight())
                    .lastActiveTime(tag.getLastUpdated())
                    .createdTime(tag.getCreatedAt())
                    .build();
            tagsDimensions.add(tagsDimension);
        }
        userProfile.setUserTagsDimensions(tagsDimensions);
        return userProfile;
    }

    public UserProfile saveProfile(UserProfile userProfile) {
        return profileRepository.save(userProfile);
    }
}
