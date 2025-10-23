package com.cloud.cloud.system.service;

import com.cloud.cloud.common.security.dto.JwtResponse;
import com.cloud.cloud.common.security.dto.LoginRequest;
import com.cloud.cloud.common.security.dto.MessageResponse;
import com.cloud.cloud.common.security.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<JwtResponse> login(LoginRequest loginRequest);

    ResponseEntity<MessageResponse> register(RegisterRequest registerRequest);

    ResponseEntity<?> getCurrentUser();

    /**
     * 根据用户ID获取token（管理员接口）
     */
    ResponseEntity<?> getTokenForUserId(Long userId, String password);
}

