package com.cloud.ai.chat.api.factory;

import com.cloud.ai.chat.api.RemoteStereotypeTagFeignService;
import com.cloud.ai.chat.api.domain.StereotypeTagRequest;
import com.cloud.ai.chat.api.domain.StereotypeTagResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象标签服务降级工厂
 * @date 2026/2/5 16:30
 */
@Slf4j
@Component
public class StereotypeTagFallbackFactory implements FallbackFactory<RemoteStereotypeTagFeignService> {

    @Override
    public RemoteStereotypeTagFeignService create(Throwable cause) {
        log.error("刻板印象标签服务调用失败: {}", cause.getMessage());
        return new RemoteStereotypeTagFeignService() {
            @Override
            public StereotypeTagResponse generateStereotypeTags(StereotypeTagRequest request) {
                return StereotypeTagResponse.fail("AI服务暂时不可用: " + cause.getMessage());
            }
        };
    }
}
