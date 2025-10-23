package com.cloud.cloud.ai.chat.domain;


import lombok.Data;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像DTO
 * @date 2025/10/18 15:38
 */
@Data
public class UserProfileRequest {
    private Long userId;
    private String gender;
    private Integer age;
    private String location;
    private Integer occupation;
    private List<String> hobbies;
}
