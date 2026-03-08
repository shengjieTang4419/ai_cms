package com.cloud.job.scanner;

import com.cloud.job.service.JobRegistryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 任务扫描器
 * 启动时扫描所有微服务的 @JobTask 注解
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobScanner {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final JobRegistryService jobRegistryService;

    @PostConstruct
    public void scanJobs() {
        log.info("开始扫描所有微服务的定时任务...");

        // 获取所有注册的服务
        List<String> services = discoveryClient.getServices();

        for (String serviceName : services) {
            if (serviceName.startsWith("cloud-")) {
                scanServiceJobs(serviceName);
            }
        }

        log.info("定时任务扫描完成");
    }

    /**
     * 扫描指定服务的任务
     */
    private void scanServiceJobs(String serviceName) {
        try {
            // 获取服务实例
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances.isEmpty()) {
                log.warn("服务 {} 没有可用实例", serviceName);
                return;
            }

            ServiceInstance instance = instances.get(0);
            String baseUrl = String.format("http://%s:%d",
                    instance.getHost(), instance.getPort());

            // 调用服务的任务扫描接口
            String scanUrl = baseUrl + "/actuator/job-scan";
            List<JobMethodInfo> jobMethods = restTemplate.getForObject(scanUrl, List.class);

            if (jobMethods != null) {
                for (JobMethodInfo jobMethod : jobMethods) {
                    registerJob(serviceName, jobMethod);
                }
            }

        } catch (Exception e) {
            log.error("扫描服务 {} 的任务失败", serviceName, e);
        }
    }

    /**
     * 注册任务到XXL-JOB
     */
    private void registerJob(String serviceName, JobMethodInfo jobMethod) {
        try {
            jobRegistryService.registerJob(
                    serviceName,
                    jobMethod.getClassName(),
                    jobMethod.getMethodName(),
                    jobMethod.getDescription(),
                    jobMethod.getCron(),
                    jobMethod.getAuthor()
            );

            log.info("注册任务: {} - {}.{}",
                    serviceName, jobMethod.getClassName(), jobMethod.getMethodName());

        } catch (Exception e) {
            log.error("注册任务失败: {} - {}.{}",
                    serviceName, jobMethod.getClassName(), jobMethod.getMethodName(), e);
        }
    }

    /**
     * 任务方法信息
     */
    public static class JobMethodInfo {
        private String className;
        private String methodName;
        private String description;
        private String cron;
        private String author;
        private String param;
        private int timeout;
        private int failRetryCount;

        // getters and setters
        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getFailRetryCount() {
            return failRetryCount;
        }

        public void setFailRetryCount(int failRetryCount) {
            this.failRetryCount = failRetryCount;
        }
    }
}
