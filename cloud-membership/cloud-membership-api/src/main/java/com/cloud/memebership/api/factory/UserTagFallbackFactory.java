package com.cloud.memebership.api.factory;

import com.cloud.common.core.response.Result;
import com.cloud.memebership.api.RemoteUserTagFeignService;
import com.cloud.memebership.domain.UserTagDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签服务降级工厂
 * @date 2025/12/09
 */
@Slf4j
@Component
public class UserTagFallbackFactory implements FallbackFactory<RemoteUserTagFeignService> {

    @Override
    public RemoteUserTagFeignService create(Throwable cause) {
        log.error("用户标签服务调用失败，原因：{}", cause.getMessage());

        return new RemoteUserTagFeignService() {
            @Override
            public Result<List<UserTagDTO>> getUserHotTags(Long userId, Integer limit) {
                log.error("获取用户热门标签失败，userId: {}, 使用降级策略", userId);
                return Result.error("用户标签服务暂时不可用，请稍后重试");
            }
        };
    }
}
