package com.cloud.system.api.feign;


import com.cloud.common.core.constant.ServiceNameConstants;
import com.cloud.common.core.domain.R;
import com.cloud.system.api.dto.LoginUser;
import com.cloud.system.api.factory.RemoteUserFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 远程用户调度服务
 * @date 2025/12/19 19:55
 */
@FeignClient(contextId = "remoteUserService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteUserFallbackFactory.class)
public interface RemoteUserService {

    @GetMapping("/user/info/{username}")
    public R<LoginUser> info(@PathVariable("username") String username);

}
