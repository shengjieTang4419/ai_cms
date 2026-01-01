package com.cloud.system.api.factory;

import com.cloud.common.core.domain.R;
import com.cloud.system.api.dto.LoginUser;
import com.cloud.system.api.feign.RemoteUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务降级处理
 * 并非所有的服务都需要降级
 * 对于关键高频的调用采用降级策略
 * 关于如何区分关键高频调用，一方面看架构设计之初的业务形态，一方面看上线后的实际调用
 * 第二点：降级策略不应该简简单单的一个返回，应该有着多级策略。
 * 注入：cacheLoad ->redisLoad -> DBLoad -> 游客模式
 * 这里先仅仅作为一个预留的埋点吧
 *
 * @author shengjie.tang
 */
@Component
public class RemoteUserFallbackFactory implements FallbackFactory<RemoteUserService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteUserFallbackFactory.class);

    @Override
    public RemoteUserService create(Throwable throwable) {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        return new RemoteUserService() {
            @Override
            public R<LoginUser> info(String username) {
                return R.fail("获取用户失败:" + throwable.getMessage());
            }

            @Override
            public R<LoginUser> register(LoginUser user) {
                return R.fail("注册用户失败:" + throwable.getMessage());
            }
        };
    }
}
