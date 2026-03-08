package com.cloud.job.service;

import com.cloud.job.domain.XxlJobInfoEntity;
import com.cloud.job.repository.XxlJobInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务注册服务
 * 负责将扫描到的任务注册到XXL-JOB数据库
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobRegistryService {

    private final XxlJobInfoRepository xxlJobInfoRepository;

    /**
     * 注册任务到XXL-JOB
     */
    public void registerJob(String serviceName, String className, String methodName,
                           String description, String cron, String author) {

        try {
            // 构建执行器参数
            String executorParam = buildExecutorParam(serviceName, className, methodName);

            // 检查任务是否已存在
            xxlJobInfoRepository.findByJobDescAndExecutorParam(description, executorParam)
                    .ifPresentOrElse(
                            existingJob -> {
                                log.info("任务已存在，更新: {} - {}.{}", serviceName, className, methodName);
                                updateExistingJob(existingJob, cron);
                            },
                            () -> {
                                log.info("注册新任务: {} - {}.{}", serviceName, className, methodName);
                                createNewJob(serviceName, className, methodName, description, cron, author, executorParam);
                            }
                    );

        } catch (Exception e) {
            log.error("注册任务失败: {} - {}.{}", serviceName, className, methodName, e);
            throw e;
        }
    }

    /**
     * 创建新任务
     */
    private void createNewJob(String serviceName, String className, String methodName,
                              String description, String cron, String author, String executorParam) {
        XxlJobInfoEntity jobInfo = new XxlJobInfoEntity();
        jobInfo.setJobGroup(1); // 默认组
        jobInfo.setJobDesc(description);
        jobInfo.setJobCron(cron);
        jobInfo.setAuthor(author);
        jobInfo.setExecutorHandler("reflectionJobHandler");
        jobInfo.setExecutorParam(executorParam);
        jobInfo.setExecutorTimeout(3600);
        jobInfo.setTriggerStatus(1); // 启动状态
        jobInfo.setGlueType("BEAN");
        jobInfo.setGlueRemark("自动注册任务");

        xxlJobInfoRepository.save(jobInfo);
    }

    /**
     * 更新现有任务
     */
    private void updateExistingJob(XxlJobInfoEntity existingJob, String cron) {
        existingJob.setJobCron(cron);
        existingJob.setUpdateTime(LocalDateTime.now());
        existingJob.setGlueUpdatetime(LocalDateTime.now());

        xxlJobInfoRepository.save(existingJob);
    }

    /**
     * 构建执行器参数
     * 格式: serviceName|className|methodName
     */
    private String buildExecutorParam(String serviceName, String className, String methodName) {
        return String.format("%s|%s|%s", serviceName, className, methodName);
    }

    /**
     * 删除任务
     */
    public void deleteJob(String serviceName, String className, String methodName) {
        String executorParam = buildExecutorParam(serviceName, className, methodName);
        
        xxlJobInfoRepository.findByJobDescAndExecutorParam("", executorParam)
                .ifPresent(job -> {
                    job.setDeleteFlag(1);
                    xxlJobInfoRepository.save(job);
                    log.info("删除任务: {} - {}.{}", serviceName, className, methodName);
                });
    }

    /**
     * 获取所有活跃任务
     */
    public java.util.List<XxlJobInfoEntity> getAllActiveJobs() {
        return xxlJobInfoRepository.findAllActive();
    }

    /**
     * 根据服务名获取任务
     */
    public java.util.List<XxlJobInfoEntity> getJobsByService(String serviceName) {
        return xxlJobInfoRepository.findByServiceName(serviceName + "|");
    }
}
