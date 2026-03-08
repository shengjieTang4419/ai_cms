package com.cloud.memebership.domain;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description:
 * @date 2026/1/11 18:27
 */
@Data
public class TagPath implements Serializable {

    /**
     * ["人工智能", "多模态", "视觉理解"]
     */
    private List<String> path;

    /**
     * 预计算的 pathKey: 人工智能/多模态/视觉理解
     */
    private String key;
}
