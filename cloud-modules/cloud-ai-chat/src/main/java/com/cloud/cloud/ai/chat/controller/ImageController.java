package com.cloud.cloud.ai.chat.controller;

import com.cloud.cloud.ai.chat.service.ImageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片上传控制器
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 处理图片上传请求
 * @date 2025/1/27
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageService imageService;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name:images}")
    private String bucketName;

    /**
     * 上传单个图片
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        
        log.info("收到图片上传请求: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        
        ImageService.ImageUploadResult result = imageService.uploadImage(file, userId);
        
        Map<String, Object> response = new HashMap<>();
        if (result.isSuccess()) {
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("fileUrl", result.getFileUrl());
            response.put("imageId", result.getImage().getId());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", result.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 批量上传图片
     */
    @PostMapping("/upload/batch")
    public ResponseEntity<Map<String, Object>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        
        log.info("收到批量图片上传请求: {} 张图片", files.size());
        
        List<ImageService.ImageUploadResult> results = imageService.uploadImages(files, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "批量上传完成");
        response.put("results", results);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 从base64上传图片
     */
    @PostMapping("/upload/base64")
    public ResponseEntity<Map<String, Object>> uploadImageFromBase64(
            @RequestParam("base64") String base64Data,
            @RequestParam(value = "contentType", defaultValue = "image/png") String contentType,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        
        log.info("收到Base64图片上传请求");
        
        ImageService.ImageUploadResult result = imageService.uploadImageFromBase64(base64Data, contentType, userId);
        
        Map<String, Object> response = new HashMap<>();
        if (result.isSuccess()) {
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("fileUrl", result.getFileUrl());
            response.put("imageId", result.getImage().getId());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", result.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 访问MinIO中的图片
     * URL格式: /api/images/2025/01/27/uuid.png
     */
    @GetMapping("/**")
    public ResponseEntity<byte[]> getImage(HttpServletRequest request) {
        try {
            // 从请求路径中提取对象名
            String requestPath = request.getRequestURI();
            String objectName = extractObjectNameFromPath(requestPath);
            
            if (objectName == null || objectName.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 从MinIO获取对象
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            // 读取数据
            byte[] data = stream.readAllBytes();
            stream.close();

            // 设置Content-Type
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            
            // 根据文件扩展名设置Content-Type
            if (objectName.endsWith(".jpg") || objectName.endsWith(".jpeg")) {
                headers.setContentType(MediaType.IMAGE_JPEG);
            } else if (objectName.endsWith(".gif")) {
                headers.setContentType(MediaType.IMAGE_GIF);
            } else if (objectName.endsWith(".webp")) {
                headers.setContentType(MediaType.parseMediaType("image/webp"));
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);

        } catch (Exception e) {
            log.error("获取图片失败", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 从请求路径中提取对象名
     * 例如: /api/images/2025/01/27/uuid.png -> 2025/01/27/uuid.png
     */
    private String extractObjectNameFromPath(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) {
            return null;
        }
        
        // 移除 /api/images 前缀
        String prefix = "/api/images/";
        if (requestPath.startsWith(prefix)) {
            return requestPath.substring(prefix.length());
        }
        
        // 如果以 /api/images 结尾，返回null
        if (requestPath.equals("/api/images")) {
            return null;
        }
        
        return requestPath;
    }
}

