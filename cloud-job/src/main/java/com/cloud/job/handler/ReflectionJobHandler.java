package com.cloud.job.handler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 反射任务处理器
 * 通过反射调用各业务服务的任务方法
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReflectionJobHandler {
    
    /**
     * 反射任务处理器
     * 参数格式: serviceName|className|methodName|param1,param2,...
     */
    @XxlJob("reflectionJobHandler")
    public void executeReflectionJob() {
        String param = XxlJobHelper.getJobParam();
        log.info("执行反射任务，参数: {}", param);
        
        try {
            // 解析参数
            String[] parts = param.split("\\|");
            if (parts.length < 3) {
                log.error("参数格式错误，期望: serviceName|className|methodName|params");
                XxlJobHelper.handleFail("参数格式错误");
                return;
            }
            
            String serviceName = parts[0];
            String className = parts[1];
            String methodName = parts[2];
            String methodParams = parts.length > 3 ? parts[3] : "";
            
            // 获取目标服务的Bean
            Object serviceBean = getServiceBean(serviceName);
            if (serviceBean == null) {
                log.error("找不到服务: {}", serviceName);
                XxlJobHelper.handleFail("找不到服务: " + serviceName);
                return;
            }
            
            // 获取目标类
            Class<?> targetClass = Class.forName(className);
            
            // 获取目标方法
            Method targetMethod = getTargetMethod(targetClass, methodName, methodParams);
            if (targetMethod == null) {
                log.error("找不到方法: {}.{}", className, methodName);
                XxlJobHelper.handleFail("找不到方法: " + className + "." + methodName);
                return;
            }
            
            // 执行方法
            Object result = targetMethod.invoke(serviceBean);
            
            log.info("任务执行成功: {}.{}", className, methodName);
            XxlJobHelper.handleSuccess("任务执行成功");
            
        } catch (Exception e) {
            log.error("任务执行失败", e);
            XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取服务Bean
     * 这里可以通过Spring ApplicationContext获取
     */
    private Object getServiceBean(String serviceName) {
        // TODO: 实现服务Bean获取逻辑
        // 可以通过ApplicationContext获取，或者通过反射调用
        // 这里简化处理，实际需要根据你的架构调整
        return null;
    }
    
    /**
     * 获取目标方法
     */
    private Method getTargetMethod(Class<?> targetClass, String methodName, String methodParams) {
        try {
            // 简单处理：查找无参方法
            return targetClass.getMethod(methodName);
            
        } catch (NoSuchMethodException e) {
            // 尝试查找带参数的方法
            Method[] methods = targetClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    return method;
                }
            }
            return null;
        }
    }
}
