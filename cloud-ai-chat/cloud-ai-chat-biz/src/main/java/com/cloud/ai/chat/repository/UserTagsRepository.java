package com.cloud.ai.chat.repository;

import com.cloud.ai.chat.domain.UserTags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签数据访问层
 * @date 2025/10/14 14:50
 */
@Repository
public interface UserTagsRepository extends JpaRepository<UserTags, Long> {

    /**
     * 根据用户ID查找用户标签，按总权重倒序，返回前5个
     */
    List<UserTags> findTop5ByUserIdOrderByTotalWeightDesc(Long userId);


    /**
     * 统计用户标签总数
     */
    long countByUserId(Long userId);

    /**
     * 删除用户的所有标签
     */
    void deleteByUserId(Long userId);
}
