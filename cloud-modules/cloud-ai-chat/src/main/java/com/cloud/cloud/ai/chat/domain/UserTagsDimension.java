package com.cloud.cloud.ai.chat.domain;


import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签维度
 * @date 2025/10/24 17:52
 */
@Data
@Builder
public class UserTagsDimension implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tagName;

    private BigDecimal totalWeight;


}
