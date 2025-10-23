package com.cloud.cloud.ai.chat.repository;

import com.cloud.cloud.ai.chat.domain.ChatTags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 聊天标签数据访问层
 * @date 2025/10/14 14:45
 */
@Repository
public interface ChatTagsRepository extends JpaRepository<ChatTags, Long> {

    /**
     * 根据用户ID查找聊天标签
     */
    List<ChatTags> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据会话ID查找聊天标签
     */
    List<ChatTags> findBySessionIdOrderByWeightDesc(String sessionId);

    /**
     * 根据用户ID和标签名查找
     */
    List<ChatTags> findByUserIdAndTagName(Long userId, String tagName);

    /**
     * 统计用户标签频率
     */
    @Query("SELECT ct.tagName, SUM(ct.frequency) as totalFrequency " +
           "FROM ChatTags ct WHERE ct.userId = :userId " +
           "GROUP BY ct.tagName ORDER BY totalFrequency DESC")
    List<Object[]> findTagFrequencyByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的所有聊天标签
     */
    void deleteByUserId(Long userId);

    /**
     * 删除会话的所有聊天标签
     */
    void deleteBySessionId(String sessionId);
}
