package com.cloud.gateway.filter;

import com.cloud.common.core.constant.CacheConstants;
import com.cloud.common.core.constant.DeviceType;
import com.cloud.common.core.constant.SecurityConstants;
import com.cloud.common.core.constant.TokenStrategy;
import com.cloud.common.core.util.JwtUtils;
import com.cloud.common.core.util.StringUtils;
import com.cloud.gateway.config.properties.IgnoreWhiteProperties;
import com.cloud.gateway.utils.RequestAdapter;
import com.cloud.gateway.utils.SecurityUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Map;

/**
 * 认证过滤器 - JWT+Redis双重验证
 * 统一处理Token验证和用户信息传递
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private final IgnoreWhiteProperties ignoreWhiteProperties;
    private final RedissonClient redisson;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // 用户信息传递Header
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_KEY_HEADER = "X-User-Key";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String requestUri = request.getPath().value();

        // 检查是否在白名单中
        if (SecurityUtils.isIgnoreUrl(requestUri, ignoreWhiteProperties.getWhites())) {
            return chain.filter(exchange);
        }

        // 获取Token
        String token = getToken(request);
        if (StringUtils.isEmpty(token)) {
            return unauthorizedResponse(response, "令牌不能为空");
        }

        // JWT+Redisson双重验证
        return validateTokenWithRedisson(exchange, token, chain);
    }

    /**
     * 从请求中获取Token
     */
    private String getToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTH_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * JWT+Redisson双重验证Token
     */
    private Mono<Void> validateTokenWithRedisson(ServerWebExchange exchange, String token, GatewayFilterChain chain) {
        try {
            // 1. JWT验证 - 解析Token获取基本信息
            Claims claims = JwtUtils.parseToken(token);
            if (claims == null) {
                return unauthorizedResponse(exchange.getResponse(), "令牌验证不正确！");
            }

            // 1.5. 检查JWT是否过期（关键修复）
            if (isJwtExpired(claims)) {
                log.debug("JWT已过期，需要刷新Token");
                return tokenExpiredResponse(exchange.getResponse(), "令牌已过期，请刷新");
            }

            // 2. 获取JWT中的用户信息
            String userKey = JwtUtils.getUserKey(claims);
            String userId = JwtUtils.getUserId(claims);
            String username = JwtUtils.getUserName(claims);

            if (StringUtils.isEmpty(userKey) || StringUtils.isEmpty(userId) || StringUtils.isEmpty(username)) {
                return unauthorizedResponse(exchange.getResponse(), "令牌验证失败");
            }

            // 3. Redisson验证 - 检查登录状态
            String redisKey = CacheConstants.LOGIN_TOKEN_KEY + userKey;
            RBucket<Map<String, Object>> bucket = redisson.getBucket(redisKey);
            boolean isLogin = bucket.isExists();
            if (!isLogin) {
                return unauthorizedResponse(exchange.getResponse(), "登录状态已过期");
            }

            // 4. 获取完整用户信息用于后续处理
            Map<String, Object> loginUser = bucket.get();
            if (loginUser == null) {
                return unauthorizedResponse(exchange.getResponse(), "用户信息不存在");
            }

            //5. 设备端校验
            if (!validateHighSecurity(exchange, loginUser)) {
                return unauthorizedResponse(exchange.getResponse(), "检测到设备变化，请重新登录");
            }

            // 6. 添加Token过期预警头
            ServerHttpResponse response = exchange.getResponse();
            addTokenExpirationWarningIfNeeded(loginUser, response);

            // 7. 请求频率限制（预留，结合Sentinel）
            // if (!checkRequestFrequency(loginUser, exchange)) {
            //     return unauthorizedResponse(exchange.getResponse(), "请求频率过高");
            // }


            // 8. 构建新的请求，添加用户信息Header
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header(USER_ID_HEADER, userId)
                    .header(USERNAME_HEADER, URLEncoder.encode(username, StandardCharsets.UTF_8))
                    .header(USER_KEY_HEADER, userKey)
                    // 清除内部请求来源参数
                    .headers(headers -> headers.remove(SecurityConstants.FROM_SOURCE))
                    .build();

            log.debug("Token验证成功，用户: {} ({})", username, userId);

            return chain.filter(exchange.mutate().request(modifiedRequest).response(response).build());

        } catch (Exception e) {
            log.error("Token验证异常: {}", e.getMessage());
            return unauthorizedResponse(exchange.getResponse(), "令牌验证失败");
        }
    }

    /**
     * 未授权响应 - 需要重新登录
     */
    private Mono<Void> unauthorizedResponse(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String body = String.format("{\"code\":%d,\"msg\":\"%s\"}", HttpStatus.UNAUTHORIZED.value(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Token过期响应 - 可以刷新Token
     */
    private Mono<Void> tokenExpiredResponse(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.PROXY_AUTHENTICATION_REQUIRED); // 407状态码
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String body = String.format("{\"code\":%d,\"msg\":\"%s\"}", 407, message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -10; // 在XssFilter之前执行
    }

    /**
     * 检查JWT是否过期
     */
    private boolean isJwtExpired(Claims claims) {
        try {
            // 检查过期时间
            if (claims.getExpiration() != null) {
                long currentTime = System.currentTimeMillis();
                long expirationTime = claims.getExpiration().getTime();
                return currentTime >= expirationTime;
            }
            return false;
        } catch (Exception e) {
            log.error("检查JWT过期时间异常: {}", e.getMessage());
            return true; // 异常情况认为已过期，更安全
        }
    }

    /**
     * 设备信息验证
     */
    private boolean validateHighSecurity(ServerWebExchange exchange, Map<String, Object> loginUser) {
        try {
            // 获取Redis中存储的设备ID
            String cachedDeviceId = (String) loginUser.get("deviceId");

            // 验证当前请求的设备信息是否与缓存中的设备信息一致
            boolean isMatch = RequestAdapter.isDeviceMatch(cachedDeviceId, exchange.getRequest());
            if (!isMatch) {
                log.warn("设备验证失败 - 用户: {}, 缓存设备: {}, 请求IP: {}",
                        loginUser.get("username"), cachedDeviceId,
                        RequestAdapter.getClientIp(exchange.getRequest()));
                return false;
            }

            // 设备验证通过
            return true;
        } catch (Exception e) {
            log.error("设备验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查Token是否需要过期预警（3/4 TTL）- 支持多设备策略
     */
    private boolean shouldAddTokenExpirationWarning(Map<String, Object> loginUser) {
        try {
            // 获取token创建时间
            Long createTime = (Long) loginUser.get("createTime");
            if (createTime == null) {
                return false;
            }

            // 获取设备类型和对应的过期时间
            String deviceTypeStr = (String) loginUser.get("deviceType");
            DeviceType deviceType = deviceTypeStr != null ?
                    DeviceType.valueOf(deviceTypeStr) : DeviceType.PC;

            Long accessTokenExpire = (Long) loginUser.get("accessTokenExpire");
            if (accessTokenExpire == null) {
                accessTokenExpire = TokenStrategy.getAccessTokenExpire(deviceType);
            }

            long currentTime = System.currentTimeMillis();
            long tokenAge = currentTime - createTime;
            long totalLifetime = accessTokenExpire * 60 * 1000L; // 转换为毫秒

            // 计算剩余时间
            long remainingTime = totalLifetime - tokenAge;

            // 获取该设备类型的刷新阈值
            double refreshThreshold = TokenStrategy.getRefreshThreshold(deviceType);
            long warningThreshold = (long) (totalLifetime * (1.0 - refreshThreshold));

            return remainingTime <= warningThreshold && remainingTime > 0;

        } catch (Exception e) {
            log.error("Token过期预警检查异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 添加Token过期预警头
     */
    private void addTokenExpirationWarningIfNeeded(Map<String, Object> loginUser, ServerHttpResponse response) {
        if (shouldAddTokenExpirationWarning(loginUser)) {
            try {
                Long createTime = (Long) loginUser.get("createTime");
                long currentTime = System.currentTimeMillis();
                long tokenAge = currentTime - createTime;
                long totalLifetime = CacheConstants.EXPIRATION * 60 * 1000L;
                long remainingTime = totalLifetime - tokenAge;

                // 添加预警响应头，通知前端token即将过期
                response.getHeaders().add("X-Token-Warning", "expiring-soon");
                response.getHeaders().add("X-Token-Remaining", String.valueOf(remainingTime / 1000)); // 秒为单位

                log.info("Token即将过期预警 - 用户: {}, 剩余时间: {}秒",
                        loginUser.get("username"), remainingTime / 1000);

            } catch (Exception e) {
                log.error("添加Token过期预警头异常: {}", e.getMessage());
            }
        }
    }
}
