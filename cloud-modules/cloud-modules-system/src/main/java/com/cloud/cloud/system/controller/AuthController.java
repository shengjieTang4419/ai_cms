package com.cloud.cloud.system.controller;


import com.cloud.cloud.common.security.dto.LoginRequest;
import com.cloud.cloud.common.security.dto.RegisterRequest;
import com.cloud.cloud.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * 认证控制器
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        return authService.getCurrentUser();
    }

    /**
     * 根据用户ID获取token（管理员接口）
     */
    @PostMapping("/admin/token")
    public ResponseEntity<?> getTokenForUserId(@RequestParam Long userId, @RequestParam String password) {
        return authService.getTokenForUserId(userId, password);
    }
}


