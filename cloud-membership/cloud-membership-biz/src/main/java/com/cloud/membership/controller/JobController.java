package com.cloud.membership.controller;

import com.cloud.membership.service.IUserPortraitAlgorithmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务控制器
 * 提供给cloud-job扫描的接口
 */
@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final IUserPortraitAlgorithmService portraitAlgorithmService;

    /**
     * 用户标签自然衰减任务
     * 懒加载模式：已在 onValueChat 中实现，无需定时任务
     */
    //@ScheduledJob(description = "用户标签自然衰减任务（已废弃）", cron = "0 0 3 * * ?")
    public void userTagsNaturalDecay() {
        log.info("用户标签自然衰减任务已改为懒加载模式，无需定时执行");
    }

    /**
     * 用户画像坍缩任务
     */
//    @ScheduledJob(
//        description = "用户画像坍缩任务",
//        cron = "0 0 2 * * SUN",
//        author = "admin"
//    )
    public void userPortraitCollapse() {
        log.info("开始执行用户画像坍缩任务");

        // 执行坍缩逻辑
        // portraitAlgorithmService.collapse(userPortraitDTO);

        log.info("用户画像坍缩任务执行完成");
    }

    /**
     * 清理过期数据任务
     */
//    @ScheduledJob(
//        description = "清理过期数据任务",
//        cron = "0 0 1 1 * ?",
//        author = "admin"
//    )
    public void cleanExpiredData() {
        log.info("开始执行清理过期数据任务");

        // 清理过期的对话记录、标签等

        log.info("清理过期数据任务执行完成");
    }
}
