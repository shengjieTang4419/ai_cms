package com.cloud.membership.controller;


import com.cloud.common.security.SecurityUtils;
import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.domain.UserProfileRequest;
import com.cloud.membership.service.impl.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像控制器
 * @date 2025/10/14 15:45
 */
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 获取当前用户画像
     */
    @GetMapping
    public UserProfile getCurrentUserProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userProfileService.getUserProfile(userId);
    }

    /**
     * 创建或更新用户画像
     */
    @PostMapping
    public UserProfile createOrUpdateProfile(@RequestBody UserProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        request.setUserId(userId);
        return userProfileService.createOrUpdateProfile(request);
    }
}
