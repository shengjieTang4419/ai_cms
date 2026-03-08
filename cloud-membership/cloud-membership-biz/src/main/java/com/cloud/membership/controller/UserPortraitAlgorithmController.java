package com.cloud.membership.controller;


import com.cloud.common.core.response.Result;
import com.cloud.membership.service.IUserPortraitAlgorithmService;
import com.cloud.memebership.domain.ValueChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/api/user/portrait/algorithm")
@RequiredArgsConstructor
public class UserPortraitAlgorithmController {

    private final IUserPortraitAlgorithmService userPortraitAlgorithmService;

    @PostMapping("/valueChat")
    public Result<Void> onValueChat(@RequestBody ValueChat valueChat) {
        try {
            userPortraitAlgorithmService.onValueChat(valueChat);
            return Result.success();
        } catch (Exception e) {
            log.error("用户画像算法处理对话失败", e);
            return Result.error("用户画像算法处理对话失败: " + e.getMessage());
        }
    }
}
