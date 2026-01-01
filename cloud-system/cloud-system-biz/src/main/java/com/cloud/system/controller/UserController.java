package com.cloud.system.controller;


import com.cloud.common.core.domain.R;
import com.cloud.common.core.web.controller.BaseController;
import com.cloud.system.api.dto.LoginUser;
import com.cloud.system.domain.User;
import com.cloud.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
        builder.userId(sysUser.get().getId());
        builder.userName(sysUser.get().getUsername());
        builder.password(sysUser.get().getPassword());
        builder.status(sysUser.get().getStatus());
        return R.ok(builder.build());
    }

    @PostMapping("/register")
    public R<LoginUser> register(@RequestBody LoginUser user) {
        //实际上这段代码如果仅仅作为remote调用 因为有fallBack机制 是没有必要加try catch的
        //但考虑到这样 更加符合代码书写习惯 也有更加清晰的业务语义 这里建议增加
        try {
            User register = userService.register(user);
            LoginUser loginUser = new LoginUser();
            loginUser.setEmail(register.getEmail());
            loginUser.setUserName(register.getUsername());
            loginUser.setUserId(register.getId());
            return R.ok(loginUser);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }


}
