package com.cloud.cloud.ai.chat.controller;

import com.cloud.cloud.ai.chat.domain.OccupationEntity;
import com.cloud.cloud.ai.chat.service.OccupationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 职业信息控制器
 * @date 2025/10/24 00:00
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/occupations")
@RequiredArgsConstructor
@Slf4j
public class OccupationController {

    private final OccupationService occupationService;

    /**
     * 获取所有启用的职业列表
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllOccupations() {
        log.info("获取所有启用的职业列表");
        List<OccupationEntity> occupations = occupationService.getAllActiveOccupations();
        
        // 转换为简化的DTO格式
        List<Map<String, Object>> result = occupations.stream()
                .map(occupation -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("code", occupation.getCode());
                    map.put("name", occupation.getName());
                    map.put("tags", occupation.getTags());
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 根据职业代码获取职业信息
     */
    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> getOccupationByCode(@PathVariable Integer code) {
        log.info("根据职业代码获取职业信息: {}", code);
        return occupationService.getByCode(code)
                .map(occupation -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("code", occupation.getCode());
                    map.put("name", occupation.getName());
                    map.put("tags", occupation.getTags());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据职业代码获取标签列表
     */
    @GetMapping("/{code}/tags")
    public ResponseEntity<List<String>> getTagsByCode(@PathVariable Integer code) {
        log.info("根据职业代码获取标签列表: {}", code);
        List<String> tags = occupationService.getTagsByCode(code);
        return ResponseEntity.ok(tags);
    }
}

