package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.domain.Image;
import com.cloud.cloud.ai.chat.repository.ImageRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 图片服务 - 使用MinIO对象存储
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 处理图片上传和存储
 * @date 2025/1/27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name:images}")
    private String bucketName;

    @Value("${app.base-url:http://localhost:18080}")
    private String baseUrl;

    /**
     * 确保Bucket存在
     */
    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建MinIO Bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO Bucket检查失败", e);
            throw new RuntimeException("MinIO连接失败", e);
        }
    }

    /**
     * 上传单个图片
     */
    public ImageUploadResult uploadImage(MultipartFile file, Long userId) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ImageUploadResult.failure("文件为空");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ImageUploadResult.failure("文件必须是图片格式");
            }

            // 确保Bucket存在
            ensureBucketExists();

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID() + extension;

            // 生成存储路径（按日期组织）
            LocalDateTime now = LocalDateTime.now();
            String datePath = now.toLocalDate().toString().replace("-", "/");
            String objectName = datePath + "/" + fileName;

            // 上传到MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );

            // 构建文件URL
            String fileUrl = baseUrl + "/api/images/" + objectName;

            // 保存到数据库
            Image image = new Image();
            image.setFileName(originalFilename);
            image.setFilePath(objectName);
            image.setFileUrl(fileUrl);
            image.setParentPath(datePath);
            image.setFileSize(file.getSize());
            image.setContentType(contentType);
            image.setUserId(userId);
            image.setCreatedAt(now);
            image.setExpiresAt(null);

            image = imageRepository.save(image);

            log.info("图片上传成功: {}, 用户ID: {}", fileUrl, userId);

            return ImageUploadResult.success(image);

        } catch (Exception e) {
            log.error("图片上传失败", e);
            return ImageUploadResult.failure("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传图片
     */
    public List<ImageUploadResult> uploadImages(List<MultipartFile> files, Long userId) {
        List<ImageUploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadImage(file, userId));
        }
        return results;
    }

    /**
     * 从base64上传图片
     */
    public ImageUploadResult uploadImageFromBase64(String base64Data, String contentType, Long userId) {
        try {
            // 解析base64数据
            String[] parts = base64Data.split(",");
            if (parts.length != 2) {
                return ImageUploadResult.failure("Base64数据格式错误");
            }

            String data = parts[1];
            byte[] imageBytes = java.util.Base64.getDecoder().decode(data);

            // 确保Bucket存在
            ensureBucketExists();

            // 生成文件名
            String extension = getExtensionFromContentType(contentType);
            String fileName = UUID.randomUUID().toString() + extension;

            // 生成存储路径
            LocalDateTime now = LocalDateTime.now();
            String datePath = now.toLocalDate().toString().replace("-", "/");
            String objectName = datePath + "/" + fileName;

            // 上传到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                            .contentType(contentType)
                            .build()
            );

            // 构建文件URL
            String fileUrl = baseUrl + "/api/images/" + objectName;

            // 保存到数据库
            Image image = new Image();
            image.setFileName("pasted-image" + extension);
            image.setFilePath(objectName);
            image.setFileUrl(fileUrl);
            image.setParentPath(datePath);
            image.setFileSize((long) imageBytes.length);
            image.setContentType(contentType);
            image.setUserId(userId);
            image.setCreatedAt(now);
            image.setExpiresAt(null);

            image = imageRepository.save(image);

            log.info("Base64图片上传成功: {}, 用户ID: {}", fileUrl, userId);

            return ImageUploadResult.success(image);

        } catch (Exception e) {
            log.error("Base64图片上传失败", e);
            return ImageUploadResult.failure("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 根据URL查找图片
     */
    public Image findByUrl(String fileUrl) {
        return imageRepository.findByFileUrl(fileUrl).orElse(null);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".png";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 从Content-Type获取扩展名
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".png";
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
    }

    /**
     * 图片上传结果
     */
    @Data
    public static class ImageUploadResult {
        private boolean success;
        private String message;
        private Image image;
        private String fileUrl;

        public static ImageUploadResult success(Image image) {
            ImageUploadResult result = new ImageUploadResult();
            result.success = true;
            result.message = "上传成功";
            result.image = image;
            result.fileUrl = image.getFileUrl();
            return result;
        }

        public static ImageUploadResult failure(String message) {
            ImageUploadResult result = new ImageUploadResult();
            result.success = false;
            result.message = message;
            return result;
        }
    }
}
