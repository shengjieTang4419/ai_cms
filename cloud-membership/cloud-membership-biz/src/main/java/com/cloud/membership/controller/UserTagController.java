package com.cloud.membership.controller;


import com.cloud.common.core.converter.BeanConverter;
import com.cloud.common.core.response.Result;
import com.cloud.membership.domain.UserTags;
import com.cloud.membership.service.impl.UserTagService;
import com.cloud.memebership.domain.UserTagDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签服务
 * @date 2025/12/9 16:23
 */
@Slf4j
@RestController
@RequestMapping("/api/user/tag")
@RequiredArgsConstructor
public class UserTagController {
    private final UserTagService userTagService;


    /**
     * 获取用户热门标签
     *
     * @param userId 用户ID
     * @param limit  返回数量限制，默认5个
     * @return 用户标签列表
     */
    @GetMapping("/hot")
    public Result<List<UserTagDTO>> getUserHotTags(@RequestParam("userId") Long userId,
                                                   @RequestParam(value = "limit", defaultValue = "5") Integer limit) {

        log.info("获取用户热门标签，userId: {}, limit: {}", userId, limit);

        try {
            // Service 层返回 PO
            List<UserTags> hotTags = userTagService.getHotTags(userId, limit);
            // Controller 层使用 Converter 转换为 DTO
            List<UserTagDTO> dtoList = BeanConverter.convertList(hotTags, UserTagDTO.class);
            return Result.success("获取用户热门标签成功", dtoList);
        } catch (Exception e) {
            log.error("获取用户热门标签失败", e);
            return Result.error("获取用户热门标签失败: " + e.getMessage());
        }
    }

}
