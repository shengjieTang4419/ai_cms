package com.cloud.job.endpoint;

import com.cloud.job.annotation.ScheduledJob;
import com.cloud.job.scanner.JobScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务扫描端点
 * 供cloud-job扫描所有服务的任务方法
 */
@Component
@Endpoint(id = "jobScan")
@Slf4j
public class JobScanEndpoint {
    
    private final ApplicationContext applicationContext;
    
    public JobScanEndpoint(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @ReadOperation
    public List<JobScanner.JobMethodInfo> scanJobs() {
        List<JobScanner.JobMethodInfo> jobMethods = new ArrayList<>();
        
        try {
            // 获取所有Bean
            String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
            
            for (String beanName : beanNames) {
                Object bean = applicationContext.getBean(beanName);
                Class<?> beanClass = bean.getClass();
                
                // 检查类是否在com.cloud包下
                if (!beanClass.getName().startsWith("com.cloud.")) {
                    continue;
                }
                
                Method[] methods = beanClass.getDeclaredMethods();
                
                for (Method method : methods) {
                    if (method.isAnnotationPresent(ScheduledJob.class)) {
                        ScheduledJob annotation = method.getAnnotation(ScheduledJob.class);
                        
                        JobScanner.JobMethodInfo jobMethod = new JobScanner.JobMethodInfo();
                        jobMethod.setClassName(beanClass.getName());
                        jobMethod.setMethodName(method.getName());
                        jobMethod.setDescription(annotation.description());
                        jobMethod.setCron(annotation.cron());
                        jobMethod.setAuthor(annotation.author());
                        jobMethod.setTimeout(annotation.timeout());
                        jobMethod.setFailRetryCount(annotation.failRetryCount());
                        
                        jobMethods.add(jobMethod);
                        
                        log.info("发现任务方法: {}.{}", beanClass.getName(), method.getName());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("扫描任务方法失败", e);
        }
        
        return jobMethods;
    }
}
