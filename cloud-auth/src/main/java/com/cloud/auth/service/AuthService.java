package com.cloud.auth.service;

import com.cloud.common.core.constant.CacheConstants;
import com.cloud.common.core.constant.UserConstants;
import com.cloud.common.core.domain.R;
import com.cloud.common.core.exception.ServiceException;
import com.cloud.common.core.util.IpUtils;
import com.cloud.common.core.util.JwtUtils;
import com.cloud.common.core.util.StringUtils;
import com.cloud.common.security.dto.LoginRequest;
import com.cloud.common.security.service.TokenService;
import com.cloud.system.api.dto.LoginUser;
import com.cloud.system.api.feign.RemoteUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 认证服务 - JWT+Redis双重验证
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 认证服务
 * @date 2025/12/13 15:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedissonClient redisson;
    private final RemoteUserService remoteUserService;
    private final SysPasswordService sysPasswordService;
    private final TokenService tokenService;

    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginRequest request, String deviceId) {
        try {
            // 调用System服务验证用户名密码
            String username = request.getUsername();
            String password = request.getPassword();

            // IP黑名单校验
            RSet<String> blackIpSet = redisson.getSet(CacheConstants.SYS_LOGIN_BLACKIPLIST);
            if (blackIpSet != null && !blackIpSet.isEmpty()) {
                String currentIp = IpUtils.getIpAddr();
                if (blackIpSet.contains(currentIp)) {
                    throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
                }
            }

            R<LoginUser> loginUserR = remoteUserService.info(username);
            if (loginUserR == null || loginUserR.getData() == null) {
                throw new BadCredentialsException("用户名或密码错误!");
            }
            if (R.FAIL == loginUserR.getCode()) {
                throw new ServiceException(loginUserR.getMsg());
            }
            LoginUser exitsUser = loginUserR.getData();

            if (exitsUser.getStatus() == 0) {
                throw new ServiceException("对不起，您的账号：" + username + " 已被删除！");
            }

            if (exitsUser.getStatus() == 2) {
                throw new ServiceException("对不起，您的账号：" + username + " 已被禁用！");
            }
            //2. 验证密码
            sysPasswordService.validate(exitsUser, password);

            // 3. 生成双Token并缓存
            Map<String, Object> tokenMap = tokenService.createToken(exitsUser, deviceId);
            log.info("用户登录成功: {} ({})", exitsUser.getUserName(), exitsUser.getUserId());
            return tokenMap;

        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            throw new ServiceException("登录失败: " + e.getMessage());
        }
    }

    /**
     * 注册
     */
    public LoginUser register(LoginUser loginUser) {
        String email = loginUser.getEmail();
        String password = loginUser.getPassword();
        String userName = loginUser.getUserName();
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(userName, password)) {
            throw new ServiceException("用户/密码必须填写");
        }
        if (userName.length() < UserConstants.USERNAME_MIN_LENGTH
                || userName.length() > UserConstants.USERNAME_MAX_LENGTH) {
            throw new ServiceException("账户长度必须在2到20个字符之间");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }

        // 注册用户信息
        LoginUser sysUser = new LoginUser();
        sysUser.setUserName(userName);
        sysUser.setPassword(password);
        sysUser.setEmail(email);
        R<LoginUser> registerResult = remoteUserService.register(sysUser);

        if (R.FAIL == registerResult.getCode()) {
            throw new ServiceException(registerResult.getMsg());
        }
        return registerResult.getData();
    }

    /**
     * 用户登出
     */
    public void logout(String token) {
        try {
            if (StringUtils.isEmpty(token)) {
                return;
            }

            // 从JWT中获取用户信息
            String userKey = JwtUtils.getUserKey(token);
            if (StringUtils.isNotEmpty(userKey)) {
                // 清除Redisson中的登录状态
                String redisKey = CacheConstants.LOGIN_TOKEN_KEY + userKey;
                RBucket<Object> bucket = redisson.getBucket(redisKey);
                bucket.delete();
                log.info("用户登出成功: {}", userKey);
            }
        } catch (Exception e) {
            log.error("登出失败: {}", e.getMessage());
        }
    }
}
