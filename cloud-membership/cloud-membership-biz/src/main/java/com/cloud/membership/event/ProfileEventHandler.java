package com.cloud.membership.event;

import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.service.IUserPortraitAlgorithmService;
import com.cloud.membership.service.impl.UserProfileService;
import com.cloud.membership.service.impl.UserTagService;
import com.cloud.memebership.domain.UserPortraitDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户画像事件处理器
 * 负责处理 UserProfileUpsertedEvent，触发相应的算法逻辑
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileEventHandler {

    private final IUserPortraitAlgorithmService algorithmService;
    private final UserProfileService userProfileService;
    private final UserTagService userTagService;

    /**
     * 处理用户画像创建/更新事件
     *
     * @param event 用户画像更新事件
     */
    @EventListener
    public void onUserProfileUpserted(UserProfileUpsertedEvent event) {
        if (event == null || event.afterProfile() == null) {
            log.warn("收到空的 UserProfileUpsertedEvent，跳过处理");
            return;
        }

        try {
            log.info("处理用户画像更新事件，用户ID: {}, 是否创建: {}",
                    event.afterProfile().getUserId(), event.created());

            // 重新加载完整的用户画像（包含标签）
            UserProfile afterProfile = userProfileService.getCurrentUserProfile(event.afterProfile().getUserId());
            if (afterProfile == null) {
                log.warn("无法加载用户画像，用户ID: {}", event.afterProfile().getUserId());
                return;
            }

            if (event.created()) {
                // 新用户：触发快速发散
                handleProfileCreated(afterProfile);
            } else {
                // 更新用户：触发重生
                handleProfileUpdated(event.beforeProfile(), afterProfile);
            }

        } catch (Exception e) {
            log.error("处理用户画像更新事件失败，用户ID: {}",
                    event.afterProfile().getUserId(), e);
        }
    }

    /**
     * 处理用户画像创建事件
     *
     * @param profile 新创建的用户画像
     */
    private void handleProfileCreated(UserProfile profile) {
        log.info("新用户画像创建，触发快速发散，用户ID: {}", profile.getUserId());
        algorithmService.divergence(profile.getUserId());
        log.info("新用户快速发散完成，用户ID: {}", profile.getUserId());
    }

    /**
     * 处理用户画像更新事件
     *
     * @param beforeProfile 更新前的画像
     * @param afterProfile  更新后的画像
     */
    private void handleProfileUpdated(UserProfile beforeProfile, UserProfile afterProfile) {
        log.info("用户画像更新，触发重生，用户ID: {}", afterProfile.getUserId());

        algorithmService.rebirth(beforeProfile, afterProfile);

        // 持久化变更
        userProfileService.saveProfile(afterProfile);
        userTagService.saveUserTags(afterProfile);

        log.info("用户画像重生完成，用户ID: {}", afterProfile.getUserId());
    }

    /**
     * 将 UserProfile 转换为 UserPortraitDTO
     *
     * @param profile 用户画像
     * @return UserPortraitDTO
     */
    private UserPortraitDTO convertToUserPortraitDTO(UserProfile profile) {
        UserPortraitDTO dto = new UserPortraitDTO();
        dto.setUserId(profile.getUserId());
        // 可以根据需要设置其他字段
        return dto;
    }
}
