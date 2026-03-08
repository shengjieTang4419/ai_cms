package com.cloud.membership.service.impl;


import com.cloud.membership.domain.TemplateProfile;
import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.domain.UserTags;
import com.cloud.membership.domain.UserTagsDimension;
import com.cloud.membership.service.ITemplateUserProfile;
import com.cloud.membership.service.IUserPortraitService;
import com.cloud.memebership.domain.UserPortraitDTO;
import com.cloud.memebership.domain.UserTagDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像服务Service
 * @date 2026/2/13 14:14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPortraitServiceImpl implements IUserPortraitService {

    private final UserProfileService userProfileService;
    private final ITemplateUserProfile templateUserProfileService;
    private final UserTagService userTagService;

    @Override
    public UserPortraitDTO getUserPortrait(Long userId) {
        return null;
    }

    @Override
    public UserPortraitDTO createUserPortrait(Long userId) {
        log.info("创建用户画像, userId: {}", userId);

        // 1. 获取用户基础画像
        UserProfile userProfile = userProfileService.getCurrentUserProfile(userId);
        if (userProfile == null) {
            log.warn("用户画像不存在, userId: {}", userId);
            return null;
        }

        // 2. 直接返回用户基础画像
        // 注意：不在此处初始化标签
        // 标签初始化由 onValueChat 中的 divergence 机制在有效对话时触发
        // 这样可以避免低置信度用户的刻板印象过早固化
        log.info("用户画像创建完成, userId: {}, 当前标签数: {}", 
                userId, 
                userProfile.getUserTagsDimensions() == null ? 0 : userProfile.getUserTagsDimensions().size());
        
        return convertToUserPortraitDTO(userProfile);
    }

    /**
     * 将 UserProfile 转换为 UserPortraitDTO
     */
    private UserPortraitDTO convertToUserPortraitDTO(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        UserPortraitDTO dto = new UserPortraitDTO();
        dto.setUserId(profile.getUserId());
        
        if (profile.getCreatedAt() != null) {
            dto.setCreatedAt(Date.from(profile.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
        }
        
        if (profile.getExpireAt() != null) {
            dto.setExpireAt(Date.from(profile.getExpireAt().atZone(ZoneId.systemDefault()).toInstant()));
        }

        // 转换热点标签
        if (!CollectionUtils.isEmpty(profile.getUserTagsDimensions())) {
            List<UserTagDTO> hotTags = new ArrayList<>();
            for (UserTagsDimension dimension : profile.getUserTagsDimensions()) {
                UserTagDTO tagDTO = new UserTagDTO();
                tagDTO.setUserId(profile.getUserId());
                tagDTO.setTagName(dimension.getPathKey());
                tagDTO.setTotalWeight(BigDecimal.valueOf(dimension.getTotalWeight()));
                
                if (dimension.getLastActiveTime() != null) {
                    tagDTO.setLastActiveTime(Date.from(dimension.getLastActiveTime().atZone(ZoneId.systemDefault()).toInstant()));
                }
                
                if (dimension.getCreatedTime() != null) {
                    tagDTO.setCreatedTime(Date.from(dimension.getCreatedTime().atZone(ZoneId.systemDefault()).toInstant()));
                }
                
                hotTags.add(tagDTO);
            }
            dto.setHotTags(hotTags);
        }

        return dto;
    }
}
