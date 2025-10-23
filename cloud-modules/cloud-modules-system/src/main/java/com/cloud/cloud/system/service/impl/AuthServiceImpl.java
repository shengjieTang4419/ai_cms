package com.cloud.cloud.system.service.impl;

import com.cloud.cloud.common.security.JwtUtils;
import com.cloud.cloud.common.security.SecurityUtils;
import com.cloud.cloud.common.security.dto.*;
import com.cloud.cloud.system.domain.User;
import com.cloud.cloud.system.service.AuthService;
import com.cloud.cloud.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:86400}")
    private int jwtExpirationMs;

    @Override
    public ResponseEntity<JwtResponse> login(LoginRequest loginRequest) {
        CustomerUserDetail userDetail = (CustomerUserDetail) userService.loadUserByUsername(loginRequest.getUsername());
        if (userDetail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), userDetail.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetail, null);
        String jwt = jwtUtils.generateJwtToken(authentication);
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetail.getUserId(),
                userDetail.getUsername(),
                userDetail.getEmail()));
    }

    @Override
    public ResponseEntity<MessageResponse> register(RegisterRequest registerRequest) {
        if (userService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }
        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        userService.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @Override
    public ResponseEntity<?> getCurrentUser() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userService.findById(currentUserId);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("createdAt", user.getCreatedAt());
        return ResponseEntity.ok(userInfo);
    }


    @Override
    public ResponseEntity<?> getTokenForUserId(Long userId, String password) {
        try {
            // 查找用户
            User user = userService.findById(userId);

            // 检查用户是否存在
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("用户不存在: " + userId));
            }

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("用户已被禁用: " + userId));
            }

            // 密码校验 - 使用Spring Security的密码编码器进行比对
            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("密码错误"));
            }

            // 创建用户详情对象
            CustomerUserDetail userDetail = new CustomerUserDetail();
            userDetail.setUserId(user.getId());
            userDetail.setUsername(user.getUsername());
            userDetail.setEmail(user.getEmail());
            userDetail.setPassword(user.getPassword());
            userDetail.setAuthorities(user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRoleCode()))
                    .collect(Collectors.toList()));

            // 生成认证对象
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());

            // 生成JWT token
            String jwt = jwtUtils.generateJwtToken(authentication);

            // 返回token信息
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtExpirationMs);
            response.put("user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("生成token失败: " + e.getMessage()));
        }
    }
}

