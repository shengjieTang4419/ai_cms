package com.cloud.ai.chat.repository;


import com.cloud.ai.chat.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 消息会话Session服务
 * @date 2025/9/24 21:49
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE ChatSession cs SET cs.messageCount = cs.messageCount + 1, cs.updatedAt = CURRENT_TIMESTAMP WHERE cs.sessionId = :sessionId")
    void incrementMessageCount(@Param("sessionId") String sessionId);

    void deleteBySessionId(String sessionId);
}
