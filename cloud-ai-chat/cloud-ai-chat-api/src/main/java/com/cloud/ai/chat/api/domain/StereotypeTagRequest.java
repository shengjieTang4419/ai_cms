package com.cloud.ai.chat.api.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象标签生成请求
 * @date 2026/2/5 16:30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StereotypeTagRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 性别：男、女
     */
    private String gender;

    /**
     * 年龄段标签
     * 例如：青年(18-25岁)
     */
    private String ageRangeLabel;

    /**
     * 年龄范围
     */
    private Integer minAge;
    private Integer maxAge;

    /**
     * 职业名称
     */
    private String occupationName;

    /**
     * 职业代码
     */
    private Integer occupationCode;

    /**
     * 居住地
     */
    private String location;

    /**
     * 爱好列表
     */
    private List<String> hobbies;
}
