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
}

