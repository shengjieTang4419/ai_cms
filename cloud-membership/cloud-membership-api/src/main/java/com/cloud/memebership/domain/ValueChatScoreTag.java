package com.cloud.memebership.domain;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 价值对话标签比分
 * @date 2026/1/3 11:54
 */
@Data
public class ValueChatScoreTag implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tag;

    private BigDecimal score;
}
