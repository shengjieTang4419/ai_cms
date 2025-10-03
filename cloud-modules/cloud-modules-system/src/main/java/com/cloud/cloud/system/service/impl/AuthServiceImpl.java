package com.cloud.cloud.system.service.impl;

import com.cloud.cloud.common.security.JwtUtils;
import com.cloud.cloud.common.security.SecurityUtils;
import com.cloud.cloud.common.security.dto.*;
import com.cloud.cloud.system.domain.User;
import com.cloud.cloud.system.service.AuthService;
import com.cloud.cloud.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

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
}

