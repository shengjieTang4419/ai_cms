package com.cloud.cloud.ai.chat.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 图片实体 - 存储上传的图片信息
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 图片实体
 * @date 2025/1/27
 */
@Data
@Document(collection = "images")
public class Image {

    @Id
    private String id;

    @Field("file_name")
    private String fileName;

    @Field("file_path")
    private String filePath;

    @Field("file_url")
    private String fileUrl;

    @Field("parent_path")
    private String parentPath;

    @Field("file_size")
    private Long fileSize;

    @Field("content_type")
    private String contentType;

    @Field("user_id")
    private Long userId;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("expires_at")
    private LocalDateTime expiresAt;

    @Field("ocr_text")
    private String ocrText;

    @Field("ocr_status")
    private String ocrStatus; // PROCESSING, SUCCESS, FAILED

    public Image() {
        this.createdAt = LocalDateTime.now();
        this.ocrStatus = "PROCESSING";
    }

    public Image(String fileName, String filePath, String fileUrl, Long fileSize, String contentType, Long userId) {
        this();
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.userId = userId;
    }
}

