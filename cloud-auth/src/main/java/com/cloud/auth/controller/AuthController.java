package com.cloud.auth.controller;

import com.cloud.auth.service.AuthService;
import com.cloud.common.core.constant.HttpStatus;
import com.cloud.common.core.domain.R;
import com.cloud.common.core.util.DeviceUtils;
import com.cloud.common.core.util.StringUtils;
import com.cloud.common.security.dto.LoginRequest;
import com.cloud.common.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 - JWT+Redis双重验证
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @description 认证服务
 * @date 2025/12/13 15:47
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // 自动从请求中提取设备信息
            String deviceId = DeviceUtils.extractDeviceId(request);
            Map<String, Object> tokenMap = authService.login(loginRequest, deviceId);
            return R.ok(tokenMap);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return R.fail(HttpStatus.UNAUTHORIZED, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (StringUtils.isNotEmpty(token)) {
                authService.logout(token);
            }
            return R.ok();
        } catch (Exception e) {
            log.error("登出失败: {}", e.getMessage());
            return R.fail("登出失败");
        }
    }

    /**
     * 刷新Token
     * <p>
     * 安全考虑：Refresh Token权限更高，放在body中不会在服务器日志中暴露
     * OAuth2标准：RFC 6749规定refresh token通过form body传递
     * 避免混淆：区分access token和refresh token的用途
     */
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String refreshToken = request.get("refresh_token");
            if (StringUtils.isEmpty(refreshToken)) {
                return R.fail(HttpStatus.UNAUTHORIZED, "RefreshToken不能为空");
            }

            // 自动从请求中提取设备信息
            String deviceId = DeviceUtils.extractDeviceId(httpRequest);

            Map<String, Object> tokenMap = tokenService.refreshToken(refreshToken, deviceId);
            return R.ok(tokenMap);
        } catch (Exception e) {
            log.error("刷新Token失败: {}", e.getMessage());
            return R.fail(HttpStatus.UNAUTHORIZED, "刷新Token失败: " + e.getMessage());
        }
    }

    /**
     * 提取Token
     */
    private String extractToken(String authHeader) {
        if (StringUtils.isEmpty(authHeader)) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
