package com.cloud.membership.repository;

import com.cloud.membership.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
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
     * 根据用户ID查找用户画像列表
     */
    List<UserProfile> findByUserId(Long userId);


    /**
     * 获取用户最新有效画像（最大版本且未过期）
     */
    @Query("SELECT up FROM UserProfile up WHERE up.userId = :userId AND up.expireAt IS NULL ORDER BY up.version DESC")
    Optional<UserProfile> findLatestValidProfile(Long userId);

    /**
     * 检查用户是否已有画像
     */
    boolean existsByUserId(Long userId);
}
