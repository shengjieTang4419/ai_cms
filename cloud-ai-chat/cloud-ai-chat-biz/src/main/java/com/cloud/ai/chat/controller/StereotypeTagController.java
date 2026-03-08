package com.cloud.ai.chat.controller;

import com.cloud.ai.chat.api.domain.StereotypeTagRequest;
import com.cloud.ai.chat.api.domain.StereotypeTagResponse;
import com.cloud.ai.chat.service.impl.StereotypeTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象标签生成控制器
 * @date 2026/2/5 16:30
 */
@RestController
@RequestMapping("/api/ai/stereotype")
@RequiredArgsConstructor
@Slf4j
public class StereotypeTagController {

    private final StereotypeTagService stereotypeTagService;

    /**
     * 根据用户特征生成刻板印象标签
     */
    @PostMapping("/generateTags")
    public StereotypeTagResponse generateStereotypeTags(@RequestBody StereotypeTagRequest request) {
        log.info("生成刻板印象标签请求: gender={}, ageRange={}, occupation={}",
                request.getGender(), request.getAgeRangeLabel(), request.getOccupationName());
        return stereotypeTagService.generateStereotypeTags(request);
    }
}
