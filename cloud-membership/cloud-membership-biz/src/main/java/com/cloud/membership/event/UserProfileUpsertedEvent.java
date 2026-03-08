package com.cloud.membership.event;

import com.cloud.membership.domain.UserProfile;

/**
 * 用户画像创建/更新事件
 */
public record UserProfileUpsertedEvent(boolean created, UserProfile beforeProfile, UserProfile afterProfile) {

}
