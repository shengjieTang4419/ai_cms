package com.cloud.ai.chat.api;

import com.cloud.ai.chat.api.domain.StereotypeTagRequest;
import com.cloud.ai.chat.api.domain.StereotypeTagResponse;
import com.cloud.ai.chat.api.factory.StereotypeTagFallbackFactory;
import com.cloud.common.core.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象标签生成远程服务接口
 * @date 2026/2/5 16:30
 */
@FeignClient(contextId = "remoteStereotypeTagFeignService",
        value = ServiceNameConstants.AI_CHAT_SERVICE,
        fallbackFactory = StereotypeTagFallbackFactory.class)
public interface RemoteStereotypeTagFeignService {

    /**
     * 根据用户特征生成刻板印象标签
     *
     * @param request 用户特征请求
     * @return 生成的标签响应
     */
    @PostMapping("/api/ai/stereotype/generateTags")
    StereotypeTagResponse generateStereotypeTags(@RequestBody StereotypeTagRequest request);
}
