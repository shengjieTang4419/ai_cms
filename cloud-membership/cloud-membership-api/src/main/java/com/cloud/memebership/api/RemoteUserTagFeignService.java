package com.cloud.memebership.api;

import com.cloud.common.core.constant.ServiceNameConstants;
import com.cloud.common.core.response.Result;
import com.cloud.memebership.api.factory.UserTagFallbackFactory;
import com.cloud.memebership.domain.UserTagDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签远程服务接口
 * @date 2025/12/09
 */
@FeignClient(contextId = "remoteUserTagFeignService", value = ServiceNameConstants.MEMBERSHIP_SERVICE, fallbackFactory = UserTagFallbackFactory.class)
public interface RemoteUserTagFeignService {

    /**
     * 获取用户热门标签
     *
     * @param userId 用户ID
     * @param limit  返回数量限制，默认5个
     * @return 用户标签列表
     */
    @GetMapping("/api/user/tag/hot")
    Result<List<UserTagDTO>> getUserHotTags(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit
    );
}
