package com.cloud.ai.chat.controller;


import com.cloud.ai.chat.domain.UserProfile;
import com.cloud.ai.chat.service.impl.UserProfileService;
import com.cloud.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像控制器
 * @date 2025/10/14 15:45
 * todo 挪动到membership服务 此代码被废弃
 */
@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
@Deprecated
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
}
