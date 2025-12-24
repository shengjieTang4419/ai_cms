package com.cloud.ai.chat.repository;

import com.cloud.ai.chat.domain.Image;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 图片仓库接口
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 图片仓库
 * @date 2025/1/27
 */
@Repository
public interface ImageRepository extends MongoRepository<Image, String> {

    /**
     * 根据用户ID查找图片
     */
    List<Image> findByUserId(Long userId);

    /**
     * 根据文件路径查找图片
     */
    Optional<Image> findByFilePath(String filePath);

    /**
     * 根据文件URL查找图片
     */
    Optional<Image> findByFileUrl(String fileUrl);
}

