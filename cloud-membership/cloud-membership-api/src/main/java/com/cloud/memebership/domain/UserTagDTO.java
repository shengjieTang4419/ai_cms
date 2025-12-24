package com.cloud.memebership.domain;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户标签对象
 * @date 2025/12/9 16:20
 */
@Data
public class UserTagDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String tagName;

    private BigDecimal baseWeight;

    private BigDecimal chatWeight;

    private BigDecimal fusionWeight;

    private BigDecimal totalWeight;

    private String sourceType;
}
