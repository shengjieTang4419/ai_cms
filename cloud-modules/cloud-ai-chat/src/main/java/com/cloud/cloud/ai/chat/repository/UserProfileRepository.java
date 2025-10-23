package com.cloud.cloud.ai.chat.repository;

import com.cloud.cloud.ai.chat.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像数据访问层
 * @date 2025/10/14 14:40
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * 根据用户ID查找用户画像
     */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * 检查用户是否已有画像
     */
    boolean existsByUserId(Long userId);
}
