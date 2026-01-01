package com.cloud.ai.chat.config;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2025/9/28 19:26
 */
@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {


    private final JdbcTemplate jdbcTemplate;

    private final EmbeddingModel model;

    @Bean
    public PgVectorStore vectorStore() {
        return PgVectorStore.builder(jdbcTemplate, model).build();
    }
}
