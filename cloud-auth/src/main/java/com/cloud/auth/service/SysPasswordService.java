package com.cloud.auth.service;

import com.cloud.common.core.constant.CacheConstants;
import com.cloud.common.core.exception.ServiceException;
import com.cloud.common.security.SecurityUtils;
import com.cloud.system.api.dto.LoginUser;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录密码方法
 *
 * @author shengjie.tang
 */
@Component
@RequiredArgsConstructor
public class SysPasswordService {

    private final Redisson redisService;

    /**
     * 登录账户密码错误次数缓存键名
     *
     * @param username 用户名
     * @return 缓存键key
     */
    private String getCacheKey(String username) {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    public void validate(LoginUser user, String password) {
        String username = user.getUserName();

        RBucket<Integer> redisServiceBucket = redisService.getBucket(getCacheKey(username));
        Integer retryCount = redisServiceBucket.get();

        if (retryCount == null) {
            retryCount = 0;
        }

        /**
         * 最大重试次数
         * 配置文件放入 避免nacos泄漏后 暴力攻击
         */
        int maxRetryCount = CacheConstants.PASSWORD_MAX_RETRY_COUNT;
        long lockTime = CacheConstants.PASSWORD_LOCK_TIME;
        if (retryCount >= maxRetryCount) {
            String errMsg = String.format("密码输入错误%s次，帐户锁定%s分钟", maxRetryCount, lockTime);
            throw new ServiceException(errMsg);
        }

        if (!matches(user, password)) {
            retryCount = retryCount + 1;
            redisServiceBucket.set(retryCount, Duration.ofMinutes(lockTime));
            throw new ServiceException("用户不存在/密码错误");
        } else {
            clearLoginRecordCache(username);
        }
    }

    public boolean matches(LoginUser user, String rawPassword) {
        return SecurityUtils.matchesPassword(rawPassword, user.getPassword());
    }

    public void clearLoginRecordCache(String loginName) {
        RBucket<Object> bucket = redisService.getBucket(getCacheKey(loginName));
        if (bucket.get() != null) {
            bucket.delete();
        }
    }
}
