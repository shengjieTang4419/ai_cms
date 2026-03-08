package com.cloud.memebership.api.factory;

import com.cloud.common.core.response.Result;
import com.cloud.memebership.api.RemoteUserPortraitAlgorithmFeignService;
import com.cloud.memebership.domain.ValueChat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class UserPortraitAlgorithmFallbackFactory implements FallbackFactory<RemoteUserPortraitAlgorithmFeignService> {

    @Override
    public RemoteUserPortraitAlgorithmFeignService create(Throwable cause) {
        log.error("用户画像算法服务调用失败，原因：{}", cause.getMessage());

        return new RemoteUserPortraitAlgorithmFeignService() {
            @Override
            public Result<Void> onValueChat(ValueChat valueChat) {
                log.error("用户画像算法处理对话失败，使用降级策略, userId: {}", valueChat != null ? valueChat.getUserId() : null);
                return Result.error("用户画像算法服务暂时不可用，请稍后重试");
            }
        };
    }
}
