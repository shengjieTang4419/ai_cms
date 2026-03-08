package com.cloud.memebership.api;

import com.cloud.common.core.constant.ServiceNameConstants;
import com.cloud.common.core.response.Result;
import com.cloud.memebership.api.factory.UserPortraitAlgorithmFallbackFactory;
import com.cloud.memebership.domain.ValueChat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(contextId = "remoteUserPortraitAlgorithmFeignService", value = ServiceNameConstants.MEMBERSHIP_SERVICE, fallbackFactory = UserPortraitAlgorithmFallbackFactory.class)
public interface RemoteUserPortraitAlgorithmFeignService {

    @PostMapping("/api/user/portrait/algorithm/valueChat")
    Result<Void> onValueChat(@RequestBody ValueChat valueChat);
}
