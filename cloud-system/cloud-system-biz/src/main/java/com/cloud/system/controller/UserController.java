package com.cloud.system.controller;


import com.cloud.common.core.domain.R;
import com.cloud.common.core.web.controller.BaseController;
import com.cloud.system.api.dto.LoginUser;
import com.cloud.system.domain.User;
import com.cloud.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户Controller
 * @date 2025/12/18 20:01
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;

    @GetMapping("/info/{username}")
    public R<LoginUser> info(@PathVariable("username") String username) {
        Optional<User> sysUser = userService.findByUsername(username);
        if (sysUser.isEmpty()) {
            return R.fail("用户不存在");
        }
        LoginUser.LoginUserBuilder builder = LoginUser.builder();
        builder.userName(sysUser.get().getUsername());
        builder.password(sysUser.get().getPassword());
        builder.status(sysUser.get().getStatus());
        return R.ok(builder.build());
    }


}
