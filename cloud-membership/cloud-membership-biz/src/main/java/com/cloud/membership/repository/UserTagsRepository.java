package com.cloud.membership.repository;

import com.cloud.membership.domain.UserTags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * 根据用户ID和标签名查找
     */
    Optional<UserTags> findByUserIdAndTagName(Long userId, String tagName);

    /**
     * 查找相似用户（基于共同标签）
     */
    @Query("SELECT DISTINCT ut2.userId FROM UserTags ut1 " +
            "JOIN UserTags ut2 ON ut1.tagName = ut2.tagName " +
            "WHERE ut1.userId = :userId AND ut2.userId != :userId " +
            "ORDER BY ut2.totalWeight DESC")
    List<Long> findSimilarUsers(@Param("userId") Long userId);

    /**
     * 统计用户标签总数
     */
    long countByUserId(Long userId);

    /**
     * 删除用户的所有标签
     */
    void deleteByUserId(Long userId);

    /**
     * 查找指定标签的所有用户
     */
    List<UserTags> findByTagNameOrderByTotalWeightDesc(String tagName);

    /**
     * 查找热门标签（按用户数量排序）
     */
    @Query("SELECT ut.tagName, COUNT(ut.userId) as userCount " +
            "FROM UserTags ut " +
            "GROUP BY ut.tagName " +
            "ORDER BY userCount DESC")
    List<Object[]> findPopularTags();
}
